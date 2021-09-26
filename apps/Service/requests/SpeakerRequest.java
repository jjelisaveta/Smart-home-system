package requests;

import java.io.Serializable;

/**
 *
 * @author Rica
 */
public class SpeakerRequest implements Serializable {
    private String songName;
    private int idUser;
    
    public SpeakerRequest(String sn){
        songName = sn;
    }
    
    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
    
}
