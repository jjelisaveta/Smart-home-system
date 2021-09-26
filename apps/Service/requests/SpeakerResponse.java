package requests;

import entities.*;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Rica
 */
public class SpeakerResponse implements Serializable{
    private List<Song> songs;
    private int idUser;
    
    public SpeakerResponse(List<Song> s){
        songs = s;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
    
}
