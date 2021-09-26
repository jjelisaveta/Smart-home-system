package speaker1;

/**
 *
 * @author Rica
 */
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.jms.*;
import requests.SpeakerRequest;
import entities.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import requests.SpeakerResponse;

public class Speaker extends Thread {
    
    @Resource(lookup="jms/__defaultConnectionFactory")
    public static ConnectionFactory connectionFactory;
    
    @Resource(lookup="speakerTopic1")
    public static Topic speakerTopic;
    
    @Resource(lookup="speakerResponse1")
    public static Topic speakerResponseTopic;
    
    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("Speaker1PU");
    private static  EntityManager em = emf.createEntityManager();
    
    JMSContext context;
    JMSConsumer consumer;
    JMSProducer producer;
    
    
    public Speaker(){
        context = connectionFactory.createContext();
        consumer = context.createConsumer(speakerTopic);
        producer = context.createProducer();
    }
    
    public void run(){
        while (true){
            try {
                System.out.println("cekam");
                ObjectMessage msg = (ObjectMessage)consumer.receive();
                SpeakerRequest speakerReq = (SpeakerRequest)msg.getObject();
                String type = msg.getStringProperty("type");
                switch (type){
                    case "alarm":
                        System.out.println(speakerReq.getSongName());
                        playSong(speakerReq.getSongName());
                        break;
                    case "taskAlarm":
                        long longDate = msg.getLongProperty("time");
                        Date date = new Date(longDate);
                        System.out.println(date);
                        if (checkAlarm(date, speakerReq.getIdUser())){
                            System.out.println(speakerReq.getSongName());
                            playSong(speakerReq.getSongName());
                        }
                        break;
                    case "play":
                        addSong(speakerReq);
                        playSong(speakerReq.getSongName());
                        break;
                    case "list":
                        List<Song> songs = getSongs(speakerReq.getIdUser());
                        sendSongs(songs, speakerReq.getIdUser());
                        break;
                }
            } catch (JMSException ex) {
                Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    public static void main(String[] args) {
        new Speaker().start();
    }
    
    public void addSong(SpeakerRequest req){
        User user = em.find(User.class, req.getIdUser()); 
        List<Song> songs = em.createQuery("select s from Song s where s.songPK.name=:n and s.user=:u", Song.class)
                .setParameter("n", req.getSongName())
                .setParameter("u", user)
                .getResultList();
        if (songs.size()!=0) return;
        
        Song song = new Song();
        song.setSongPK(new SongPK(req.getSongName(), req.getIdUser()));
        song.setUser(user);
        
        em.getTransaction().begin();
        em.persist(song);
        em.getTransaction().commit();
    }
    
    public List<Song> getSongs(int idUser){
        User user = em.find(User.class, idUser);        
        List<Song> songs = em.createQuery("select s from Song s where s.user=:u", Song.class)
                .setParameter("u", user)
                .getResultList();
        return songs;
    }
    
    public void sendSongs(List<Song> songs, int idUser){
        try {
            SpeakerResponse response = new SpeakerResponse(songs);
            response.setIdUser(idUser);
            ObjectMessage msg = context.createObjectMessage(response);
            msg.setIntProperty("id", idUser);
            producer.send(speakerResponseTopic, msg);
            System.out.println("poslato");
        } catch (JMSException ex) {
            Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void playSong(String songName){
        new MusicPlayer(songName).playSong();
    }
    
    public boolean checkAlarm(Date date, int idUser){
        User user = em.find(User.class, idUser);
        System.out.println(idUser);
        List<Task> tasks = em.createQuery("select t from Task t where t.idUser=:u", Task.class)
                .setParameter("u", user)
                .getResultList();
        
        for (Task task : tasks){
            if (task.getReminder()!=null && task.getReminder().getTime()==date.getTime())
                return true;
        }
        return false;
    }
}
