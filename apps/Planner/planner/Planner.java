package planner;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.TravelMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.*;
import javax.annotation.Resource;
import javax.jms.*;
import requests.*;
import entities.*;
import javax.persistence.*;

/**
 *
 * @author Rica
 */
public class Planner extends Thread {

    @Resource(lookup="jms/__defaultConnectionFactory")
    public static ConnectionFactory connectionFactory;
    
    @Resource(lookup="alarmTopic1")
    public static Topic alarmTopic;
    
    @Resource(lookup="taskTopic1")
    public static Topic taskTopic;
    
    @Resource(lookup="taskResponse1")
    public static Topic taskResponseTopic;
    
    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("PlannerPU");
    private static  EntityManager em = emf.createEntityManager();
    
    
    JMSContext context;
    JMSProducer producer;
    JMSConsumer consumer;
    
    public Planner(){
        context = connectionFactory.createContext();
        producer = context.createProducer();
        consumer = context.createConsumer(taskTopic);
        geoContext = new GeoApiContext.Builder().apiKey(API_KEY).build();
    }
    
    private static final String API_KEY = "api_key";
    private static GeoApiContext geoContext;
    
    private static long timeAB(String locationA, String locationB) {
        try {
            System.out.println(locationA + " - " + locationB);
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(geoContext);
            
            DistanceMatrix result = req.origins(locationA)
                    .destinations(locationB)
                    .mode(TravelMode.DRIVING)
                    .language("en-US")
                    .await();
            
            return result.rows[0].elements[0].duration.inSeconds ;
        } catch (ApiException | InterruptedException | IOException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    public void run(){
        
        while (true){
            try {
                ObjectMessage msg = (ObjectMessage)consumer.receive();
                TaskRequest taskReq = (TaskRequest)msg.getObject();
                String type = msg.getStringProperty("type");
                int flag = 0;
                switch (type){
                    case "create":
                        flag = isPossible(taskReq);
                        sendResponseFlag(flag, taskReq, type);
                        System.out.println(flag);
                        boolean rem = msg.getBooleanProperty("reminder");
                        if (flag==0)
                            addTask(taskReq, rem);
                        break;
                    case "remove":
                        flag = removeTask(taskReq);
                        sendResponseFlag(flag, taskReq, type);
                        break;
                    case "list":
                        List<Task> tasks = getTasks(taskReq.getIdUser());
                        sendTasks(taskReq.getIdUser(),tasks);
                        break;
                    case "edit":
                        int idTask = msg.getIntProperty("idTask");
                        flag = editTask(taskReq, idTask);
                        sendResponseFlag(flag, taskReq, type);
                        break;
                    case "timeAB":
                        sendTimeAB(taskReq);
                        break;
                }
                
            } catch (JMSException ex) {
                Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public int isPossible(TaskRequest req){
        User user = em.find(User.class, req.getIdUser());
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:id", Task.class)
                .setParameter("id", user)
                .getResultList();
        Date start = req.getStart();
        Date end1 = Date.from(start.toInstant().plusSeconds(req.getDuration()));
        
        Task prev = getPrevTask(start, tasks);
        Task next = getNextTask(start, tasks);
        
        if (prev!= null) {
            long travelTime = timeAB(prev.getLocation(), req.getDst());
            Date start1 = Date.from(start.toInstant().minusSeconds(travelTime));
            Date start2 = prev.getStart();
            Date end2 = Date.from(start2.toInstant().plusSeconds(prev.getDuration()));
            System.out.println("prev: time "+ travelTime + ", start " + start2 + " end " + end2);
            System.out.println("cur sa putovanjem :" + start1 + " - " +end1);
            
            if (start1.getTime()<= end2.getTime() && start2.getTime() <= end1.getTime())
                return 1;
        }
        
        if (next!=null){
            long travelTime = timeAB(next.getLocation(), req.getDst());
            Date start1 = start;
            Date start2 = Date.from(next.getStart().toInstant().minusSeconds(travelTime));
            Date end2 = Date.from(next.getStart().toInstant().plusSeconds(next.getDuration()));
            System.out.println("next: time "+ travelTime + ", start " + start2 + " end " + end2);
            System.out.println("cur sa putovanjem :" + start1 + " - " +end1);
            if (start1.getTime()<= end2.getTime() && start2.getTime() <= end1.getTime())
                return 1;
        }
        return 0;
    }
    
    public Task getPrevTask(Date start, List<Task> tasks){
        long tStart = start.getTime();
        long prevStart = 0;
        Task prev = null;
        for (Task task : tasks){
            if (task.getStart().getTime() < tStart && task.getStart().getTime() > prevStart){
                prev = task;
                prevStart = task.getStart().getTime();
            }
        }
        return prev;
    }
    
    public Task getNextTask(Date start, List<Task> tasks){
        long tStart = start.getTime();
        long nextStart = Long.MAX_VALUE;
        Task next = null;
        for (Task task : tasks){
            if (task.getStart().getTime() > tStart && task.getStart().getTime() < nextStart){
                next = task;
                nextStart = task.getStart().getTime();
            }
        }
        return next;
    }
    
    public void addTask(TaskRequest req, boolean rem){
        try {
            User user = em.find(User.class, req.getIdUser());
            Task newTask = new Task();
            newTask.setDuration((int) req.getDuration());
            newTask.setIdUser(user);
            newTask.setStart(req.getStart());
            if (req.getDst().equals(""))
                newTask.setLocation(user.getMyLocation());
            else
                newTask.setLocation(req.getDst());
            
            Date start = req.getStart();
            String currLoc = getLastLocation(req.getIdUser(), start);
            Date reminder = Date.from(start.toInstant().minusSeconds(timeAB(currLoc, req.getDst())));
            if (rem)
                newTask.setReminder(reminder);
            else
                newTask.setReminder(null);
            em.getTransaction().begin();
            em.persist(newTask);
            em.getTransaction().commit();
            
            if (rem){
                AlarmRequest alarmReq = new AlarmRequest("", reminder, 0, req.getIdUser());
                ObjectMessage msg = context.createObjectMessage(alarmReq);
                msg.setStringProperty("method", "taskAlarm");
                producer.send(alarmTopic, msg);
            }
        } catch (Exception ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<Task> getTasks(int idUser){
        User user = em.find(User.class, idUser);
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:id", Task.class)
                .setParameter("id", user)
                .getResultList();
        return tasks;
    }
    
    public void sendTasks(int idUser, List<Task> tasks){
        try {
            TaskResponse response = new TaskResponse(0);
            response.setTasks(tasks);
            response.setIdUser(idUser);
            
            ObjectMessage msg = context.createObjectMessage(response);
            msg.setStringProperty("type","list");
            msg.setIntProperty("id", idUser);
            producer.send(taskResponseTopic, msg);
        } catch (JMSException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int removeTask(TaskRequest req){
        User user = em.find(User.class, req.getIdUser());
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:id", Task.class)
                .setParameter("id", user)
                .getResultList();
     
        for (Task t: tasks){
            if (t.getStart().getTime()==req.getStart().getTime()){
                em.getTransaction().begin();
                em.remove(t);
                em.getTransaction().commit();
                return 0;
            }
        }
        return 1;
    }
    
    public int editTask(TaskRequest req, int idTask){
        try {
            Task task = em.find(Task.class, idTask);
           
            if (task == null)
                return 1;
            
            em.getTransaction().begin();
            em.remove(task);
            em.getTransaction().commit();
            
            Task oldTask = new Task();
            oldTask.setDuration(task.getDuration());
            oldTask.setIdTask(task.getIdTask());
            oldTask.setIdUser(task.getIdUser());
            oldTask.setLocation(task.getLocation());
            oldTask.setReminder(task.getReminder());
            oldTask.setStart(task.getStart());
            
            if (req.getStart()!=null)
                task.setStart(req.getStart());
            else
                req.setStart(task.getStart());
            
            if (req.getDuration()!=-1)
                task.setDuration((int) req.getDuration());
            else
                req.setDuration(task.getDuration());
            if (!req.getDst().equals(""))
                task.setLocation(req.getDst());
            else
                req.setDst(task.getLocation());
            
            if (isPossible(req) == 1){
                em.getTransaction().begin();
                em.persist(oldTask);
                em.getTransaction().commit();
                return 1;
            }
            
            if (req.getStart()!=null && task.getReminder()!=null){
                
                User user = em.find(User.class, req.getIdUser());
                Date start = req.getStart();
                String currLocation = getLastLocation(req.getIdUser(), start);
                Date reminder = Date.from(start.toInstant().minusSeconds(timeAB(currLocation, req.getDst())));
                task.setReminder(reminder);
                
                AlarmRequest alarmReq = new AlarmRequest(user.getMySong(), reminder, 0, req.getIdUser());
                ObjectMessage msg = context.createObjectMessage(alarmReq);
                msg.setStringProperty("method", "taskAlarm");
                producer.send(alarmTopic, msg);
            }
            
            em.getTransaction().begin();
            em.persist(task);
            em.getTransaction().commit();
            
            return 0;
        } catch (JMSException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }
    }
    
    public String getCurrentLocation(int idUser){
        User user = em.find(User.class, idUser);
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:u", Task.class)
                .setParameter("u", user).getResultList();
        Date now = new Date();
        for (Task task : tasks){
            Date end = Date.from(task.getStart().toInstant().plusSeconds(task.getDuration()));
            if (task.getStart().getTime()<=now.getTime() && end.getTime()>now.getTime())
                return task.getLocation();
        }
        return user.getMyLocation();
    }
    
    public String getLastLocation(int idUser, Date start){
        User user = em.find(User.class, idUser);
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:u", Task.class)
                .setParameter("u", user)
                .getResultList();
        long maxDate = 0;
        Task maxTask = null;
        for (Task t : tasks){
            if (t.getStart().getTime()< start.getTime() && t.getStart().getTime()>maxDate){
                maxTask = t;
                maxDate = t.getStart().getTime();
            }
        }
        if (maxTask == null)
            return user.getMyLocation();
        else
            return maxTask.getLocation();
    }
    
    public void sendResponseFlag(int flag, TaskRequest req, String type){
        try {
            TaskResponse response = new TaskResponse(flag);
            response.setIdUser(req.getIdUser());
            ObjectMessage msg = context.createObjectMessage(response);
            msg.setIntProperty("id", req.getIdUser());
            msg.setStringProperty("type", type);
            producer.send(taskResponseTopic, msg);
        } catch (JMSException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendTimeAB(TaskRequest req){
        try {
            String locA = req.getDst();
            if (locA.equals(""))
                locA = getCurrentLocation(req.getIdUser());
            String locB = req.getDstB();
            int id = req.getIdUser();
            
            long ret = timeAB(locA, locB);
            int flag = 0;
            if (ret==-1)
                flag = 1;
            TaskResponse response = new TaskResponse(flag);
            response.setIdUser(id);
            response.setTime(ret);
            ObjectMessage msg = context.createObjectMessage(response);
            msg.setStringProperty("type", "timeAB");
            msg.setIntProperty("id", id);
            producer.send(taskResponseTopic, msg);
        } catch (JMSException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String args[]){
        new Planner().start();
    }
}
