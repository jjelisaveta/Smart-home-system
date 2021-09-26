
package requests;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Rica
 */

public class TaskRequest implements Serializable {
    private Date start;
    private long duration;
    private String dst;
    private int idUser;
    private String dstB;
    
    public TaskRequest(Date s, long d, String dd, int id){
        start = s;
        duration = d;
        dst = dd;
        idUser = id;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }
    
    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getDstB() {
        return dstB;
    }

    public void setDstB(String dstB) {
        this.dstB = dstB;
    }
}
