package requests;

import entities.Task;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Rica
 */
public class TaskResponse implements Serializable {
    private int flag;
    private List<Task> tasks;
    private long time;
    private int idUser;
    
    public TaskResponse(int f){
        flag = f;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
    
    
    
}
