package services;

import entities.Task;
import entities.User;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import requests.TaskRequest;
import requests.TaskResponse;

/**
 *
 * @author Rica
 */

@Path("planner")
public class PlannerService {
    @PersistenceContext(unitName="ServicePU")
    EntityManager em;
    
    @Resource(lookup="jms/__defaultConnectionFactory")
    public ConnectionFactory connectionFactory;
    
    @Resource(lookup="taskTopic1")
    public Topic taskTopic;
    
    @Resource(lookup="taskResponse1")
    public Topic taskResponseTopic;
    
    @POST
    @Path("create")
    public Response createTask(@Context HttpHeaders httpHeaders, @FormParam("startTime") String start, @FormParam("duration") Integer duration, @FormParam("location") String location, @FormParam("reminder") String rem){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            if (duration == null)
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: No duration.").build();
           
            if (location == null)
                location = "";
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            
            Date date = getDate(start);
            if (date==null)
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Error: Wrong date format.").build();
            
            TaskRequest req = new TaskRequest(date, duration, location, id);
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "create");
            if (rem.equals("yes"))
                msg.setBooleanProperty("reminder", true);
            else 
                msg.setBooleanProperty("reminder", false);
            producer.send(taskTopic, msg);
           
            JMSConsumer consumer = context.createConsumer(taskResponseTopic, "type='create' and id="+id, false);
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            TaskResponse response = (TaskResponse)objMsg.getObject();
            String ret = "";
            if (response.getFlag() == 0) 
                ret = "Task added.";
            else
                ret = "Task can't be added.";
            return Response.ok(ret).build();
        } catch (JMSException ex) {
            Logger.getLogger(PlannerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
    
    @POST
    @Path("remove")
    public Response removeTask(@Context HttpHeaders httpHeaders, @FormParam("startTime") String start){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            
            Date date = getDate(start);
            if (date==null)
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Wrong date format.").build();
            
            TaskRequest req = new TaskRequest(date, 0, "", id);
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "remove");
            producer.send(taskTopic, msg);
            
            JMSConsumer consumer = context.createConsumer(taskResponseTopic, "type='remove' and id="+id, false);
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            TaskResponse response = (TaskResponse)objMsg.getObject();
            String ret = "";
            if (response.getFlag() == 0) 
                ret = "Task removed.";
            else
                ret = "Task does not exist.";
            
            return Response.ok(ret).build();
        } catch (JMSException ex) {
            Logger.getLogger(PlannerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
    
    @POST
    @Path("get")
    public Response getTasks(@Context HttpHeaders httpHeaders){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            
            TaskRequest req = new TaskRequest(null, 0, "", id);
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "list");
            producer.send(taskTopic, msg);
            
            
            JMSConsumer consumer = context.createConsumer(taskResponseTopic, "type='list' and id="+id, false);
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            TaskResponse response = (TaskResponse)objMsg.getObject();
            List<Task> tasks = response.getTasks();
            
            StringBuilder sb = new StringBuilder("");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy. HH:mm");
            for (Task task: tasks){
                sb.append(((Integer)task.getIdTask()).toString()).append(". start time-").append(sdf.format(task.getStart()));
                sb.append("; duration-").append(((Integer)task.getDuration()).toString());
                sb.append("; location-").append(task.getLocation());
                sb.append("\n");
            }
            return Response.ok(sb.toString()).build();
        } catch (JMSException ex) {
            Logger.getLogger(PlannerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
    
    @POST
    @Path("edit")
    public Response editTask(@Context HttpHeaders httpHeaders, @FormParam("id") Integer idTask, @FormParam("startTime") String startTime, @FormParam("duration") Integer duration, @FormParam("location") String location){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            Date date = null;
            if (startTime!= null && !startTime.equals(""))
                date = getDate(startTime);
            //date  = new Date();
            
            
            TaskRequest req = new TaskRequest(date, duration, location, id);
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "edit");
            msg.setIntProperty("idTask", idTask);
            producer.send(taskTopic, msg);
            
            JMSConsumer consumer = context.createConsumer(taskResponseTopic, "type='edit' and id="+id, false);
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            TaskResponse response = (TaskResponse)objMsg.getObject();
            String ret = "";
            if (response.getFlag() == 0)
                ret = "Task edited.";
            else
                ret = "Error: Task does not exist.";
            return Response.ok(ret).build();
        } catch (JMSException ex) {
            Logger.getLogger(PlannerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
    
    @POST
    @Path("timeAB")
    public Response getTravelTimeAB(@Context HttpHeaders httpHeaders, @FormParam("locationA") String locA, @FormParam("locationB") String locB){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            
            if (locA == null)
                locA = "";
            
            TaskRequest req = new TaskRequest(null, 0, locA, id);
            req.setDstB(locB);
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "timeAB");
            producer.send(taskTopic, msg);
        
            JMSConsumer consumer = context.createConsumer(taskResponseTopic, "type='timeAB' and id="+id, false);
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            TaskResponse response = (TaskResponse)objMsg.getObject();
            long travelTimeAB = response.getTime();
            return Response.ok(travelTimeAB).build();
        } catch (JMSException ex) {
            Logger.getLogger(PlannerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    
    }
    
    
    private Date getDate(String s){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy. HH:mm");
            Date d = sdf.parse(s);
            return d;
        } catch (ParseException ex) {
            Logger.getLogger(AlarmService.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private User getUser(HttpHeaders httpHeaders){
        List<String> authHeaderValues = httpHeaders.getRequestHeader("Authorization");
        if(authHeaderValues != null && authHeaderValues.size() > 0){
            String authHeaderValue = authHeaderValues.get(0);
            String decodedAuthHeaderValue = new String(Base64.getDecoder().decode(authHeaderValue.replaceFirst("Basic ", "")),StandardCharsets.UTF_8);
            StringTokenizer stringTokenizer = new StringTokenizer(decodedAuthHeaderValue, ":");
            String username = stringTokenizer.nextToken();
            List<User> users = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getResultList();
            if (users.size()==0)
                return null;
            else 
                return users.get(0);
        }
        return null;
    }
    
}
