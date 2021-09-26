package alarm;

import entities.User;
import requests.AlarmRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Timer;
import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.*;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import task.AlarmTask;

public class Alarm extends Thread {
    private static Timer timer;
    private String defaultSong = "alarm sound effect";
    
    @Resource(lookup="jms/__defaultConnectionFactory")
    public static ConnectionFactory connectionFactory;
    
    @Resource(lookup="alarmTopic1")
    public static Topic alarmTopic;
    
    @Resource(lookup="speakerTopic1")
    public static Topic speakerTopic;
    
    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("AlarmPU");
    private static  EntityManager em = emf.createEntityManager();
    
    JMSContext context;
    JMSConsumer consumer;
            
    public Alarm(){
        timer = new Timer();
        context = Alarm.connectionFactory.createContext();
        consumer = context.createConsumer(Alarm.alarmTopic);
    }
    
    public void setAlarm(AlarmRequest req){
        System.out.println("Treba da zvoni u " + req.getTime() +" pesma " +req.getSongName());
        timer.schedule(new AlarmTask(req, "alarm"),req.getTime());
    }
    
    public void setAlarmPeriodic(AlarmRequest req){
        timer.scheduleAtFixedRate(new AlarmTask(req, "alarm"), req.getTime(), req.getPeriod());
    }
    
    public void setTaskAlarm(AlarmRequest req){
        timer.schedule(new AlarmTask(req, "taskAlarm"),req.getTime());
    }
    
    public void setDefaultSong(AlarmRequest req){
        if (req.getSongName().equals(""))  
            req.setSongName(defaultSong);
    }
    
    public void setSong(AlarmRequest req){
        User user = em.find(User.class, req.getIdUser());
        user.setMySong(req.getSongName());
        System.out.println(req.getSongName());
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
    }
    
    public static void main(String[] args) throws InterruptedException {
        new Alarm().start();
    }
 
    public void run(){
        
        while (true){
            try {
                ObjectMessage msg = (ObjectMessage)consumer.receive();
                String method = msg.getStringProperty("method");
                AlarmRequest req = (AlarmRequest)msg.getObject();
                User user = em.find(User.class, req.getIdUser());
                
                switch(method){
                    case "set":
                        req.setSongName(user.getMySong());
                        setAlarm(req);
                        break;
                    case "setPeriodic":
                        req.setSongName(user.getMySong());
                        setAlarmPeriodic(req);
                        break;
                    case "taskAlarm":
                        req.setSongName(user.getMySong());
                        setTaskAlarm(req);
                        break;
                    case "setSong":
                        setSong(req);
                        break;  
                }
                
            } catch (JMSException ex) {
                Logger.getLogger(Alarm.class.getName()).log(Level.SEVERE, null, ex);
            }
                 
        }
    }
}
