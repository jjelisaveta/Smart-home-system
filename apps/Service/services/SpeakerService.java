package services;

import entities.Song;
import entities.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.jms.*;
import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import requests.SpeakerRequest;
import requests.SpeakerResponse;

/**
 *
 * @author Rica
 */

@Path("speaker")
public class SpeakerService {
    @PersistenceContext(unitName="ServicePU")
    EntityManager em;
    
    @Resource(lookup="jms/__defaultConnectionFactory")
    public ConnectionFactory connectionFactory;
    
    @Resource(lookup="speakerTopic1")
    public Topic speakerTopic;
    
    @Resource(lookup="speakerResponse1")
    public Topic speakerResponseTopic;
    
    @POST
    @Path("play")
    public Response playSong(@Context HttpHeaders httpHeaders, @FormParam("song") String song){
        try {
            if (song == null)
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: No song name.").build();
            
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            
            SpeakerRequest req = new SpeakerRequest(song);
            req.setIdUser(id);
            
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "play");
            producer.send(speakerTopic, msg);
            
            return Response.ok("Song is playing.").build();
        } catch (JMSException ex) {
            Logger.getLogger(SpeakerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
    
    @POST
    @Path("list")
    public Response getHistory(@Context HttpHeaders httpHeaders){
        try {
            JMSContext context = connectionFactory.createContext();
            JMSProducer producer = context.createProducer();
            
            User user = getUser(httpHeaders);
            int id = user.getIdUser();
            SpeakerRequest req = new SpeakerRequest("");
            req.setIdUser(id);
            
            ObjectMessage msg = context.createObjectMessage(req);
            msg.setStringProperty("type", "list");
            producer.send(speakerTopic, msg);
            
            JMSConsumer consumer = context.createConsumer(speakerResponseTopic, "id="+id, false);
            
            ObjectMessage objMsg = (ObjectMessage)consumer.receive();
            SpeakerResponse response = (SpeakerResponse)objMsg.getObject();
            List<Song> songs = response.getSongs();
            
            if (response.getIdUser()==id) {
                StringBuilder sb = new StringBuilder("Played songs:\n");
                for (Song song:songs){
                    sb.append("  ").append(song.getSongPK().getName()).append("\n");
                }
                return Response.ok(sb.toString()).build();
            }
            else 
                return Response.ok("Not same user.").build();
        } catch (JMSException ex) {
            Logger.getLogger(SpeakerService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
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
