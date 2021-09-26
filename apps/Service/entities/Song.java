/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Windows User
 */
@Entity
@Table(name = "song")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Song.findAll", query = "SELECT s FROM Song s"),
    @NamedQuery(name = "Song.findByName", query = "SELECT s FROM Song s WHERE s.songPK.name = :name"),
    @NamedQuery(name = "Song.findByIdUser", query = "SELECT s FROM Song s WHERE s.songPK.idUser = :idUser")})
public class Song implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SongPK songPK;
    @JoinColumn(name = "idUser", referencedColumnName = "idUser", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private User user;

    public Song() {
    }

    public Song(SongPK songPK) {
        this.songPK = songPK;
    }

    public Song(String name, int idUser) {
        this.songPK = new SongPK(name, idUser);
    }

    public SongPK getSongPK() {
        return songPK;
    }

    public void setSongPK(SongPK songPK) {
        this.songPK = songPK;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (songPK != null ? songPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Song)) {
            return false;
        }
        Song other = (Song) object;
        if ((this.songPK == null && other.songPK != null) || (this.songPK != null && !this.songPK.equals(other.songPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.Song[ songPK=" + songPK + " ]";
    }
    
}
