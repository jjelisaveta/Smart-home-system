package requests;

/**
 *
 * @author Rica
 */

import java.io.Serializable;
import java.util.Date;

public class AlarmRequest implements Serializable {
    private String songName;
    private Date time;
    private long period;
    private int idUser;
    
    public AlarmRequest(String sn, Date t, long p, int iu){
        songName = sn;
        time = t;
        period = p;
        idUser = iu;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
    
}
