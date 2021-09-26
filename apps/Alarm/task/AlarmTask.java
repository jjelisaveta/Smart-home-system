package task;

import alarm.Alarm;
import requests.AlarmRequest;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.jms.*;
import requests.SpeakerRequest;

public class AlarmTask extends TimerTask {
   
    private AlarmRequest req;
    private JMSContext context;
    private JMSProducer producer;
    private String type;
    public AlarmTask(AlarmRequest ar, String t){  
        req = ar;
        context = Alarm.connectionFactory.createContext();
        producer = context.createProducer();
        type = t;
        System.out.println("napravljeno");
    }
        
    public void run(){
        try {
            SpeakerRequest speakerReq = new SpeakerRequest(req.getSongName());
            
            speakerReq.setIdUser(req.getIdUser());
            ObjectMessage msg = context.createObjectMessage(speakerReq);
           
            if (type.equals("taskAlarm")){
                msg.setStringProperty("type", "taskAlarm");
                msg.setLongProperty("time", req.getTime().getTime());
            } else {
                msg.setStringProperty("type", "alarm");
                msg.setLongProperty("time", 0);
            }
            producer.send(Alarm.speakerTopic, msg);
            System.out.println("poslato");
        } catch (JMSException ex) {
            Logger.getLogger(AlarmTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
