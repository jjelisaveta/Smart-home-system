
package services;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import entities.*;
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
import javax.ws.rs.core.Response.Status;
import requests.*;

@Path("alarm")
public class AlarmService {
    @PersistenceContext(unitName="ServicePU")
    EntityManager em;
    
    @Resource(lookup="jms/__defaultConnectionFactory")
    public ConnectionFactory connectionFactory;
    
    @Resource(lookup="alarmTopic1")
    public Topic alarmTopic;
    
    @POST
    @Path("set")
    public Response setAlarm(@Context HttpHeaders httpHeaders, @FormParam("dateTime") String datetime, @FormParam("period")Integer period){
        try {
            User user = getUser(httpHeaders);
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            em.getEntityManagerFactory().getCache().evictAll();
            String song = "";
            /*if (song==null)
                song = "";*/
            
            
            if (period == null)
                period = 0;
            
            Date d = getDate(datetime);
            if (d==null) 
                return Response.status(Status.NOT_ACCEPTABLE).entity("Wrong date format.").build();
            
            period *= 1000;
            int id = user.getIdUser();
            AlarmRequest req = new AlarmRequest(song, d, period, id);
            ObjectMessage msg = context.createObjectMessage(req);
            String method = "setPeriodic";
            if (period==0)
                method = "set";
            msg.setStringProperty("method", method);
            producer.send(alarmTopic, msg);
            return Response.ok("Alarm is set.").build();
        } catch (JMSException ex) {
            Logger.getLogger(AlarmService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.ok("JMSException").build();
        }
    }
    
    @POST
    @Path("song")
    public Response setSong(@Context HttpHeaders httpHeaders, @FormParam("song") String songName){
        try {
            User user = getUser(httpHeaders);
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            
            AlarmRequest req = new AlarmRequest(songName, null,0, user.getIdUser());
            ObjectMessage msg = context.createObjectMessage(req);
            String method = "setSong";
            msg.setStringProperty("method", method);
            producer.send(alarmTopic, msg);
            return Response.ok("Song is set.").build();
        } catch (JMSException ex) {
            Logger.getLogger(AlarmService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.ok("JMSException").build();
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
