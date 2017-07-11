package entity.qms;

import entity.files.Qmsplan2File;
import entity.system.Commontags;
import entity.system.Users;
import interfaces.Attachable;
import op.OPDE;
import gui.GUITools;

import javax.persistence.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by tloehr on 28.05.14.
 */
@Entity
@Table(name = "qmsplan")
public class Qmsplan implements Attachable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true)
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Basic
    @Column(name = "title", nullable = false, insertable = true, updatable = true, length = 200)
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 16777215)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "state", nullable = false, insertable = true, updatable = true)
    private byte state;

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }


    @JoinColumn(name = "uid", referencedColumnName = "UKennung")
    @ManyToOne
    private Users user;

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    @Column(name = "color", nullable = false, insertable = true, updatable = true, length = 6)
    private String color;

    public Color getColor() {
        return GUITools.getColor(color);
    }

    public void setColor(Color newColor) {
        color = GUITools.getHTMLColor(newColor);
    }


    @Version
    @Column(name = "version")
    private Long version;

    @ManyToMany
    @JoinTable(name = "qmsp2tags", joinColumns =
    @JoinColumn(name = "qmspid"), inverseJoinColumns =
    @JoinColumn(name = "ctagid"))
    private Collection<Commontags> commontags;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "qmsplan", orphanRemoval = true)
    private List<Qmssched> qmsschedules;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "qmsplan")
    private Collection<Qmsplan2File> attachedFilesConnections;

    public List<Qmssched> getQmsschedules() {
        return qmsschedules;
    }

    public Qmsplan() {
    }

    public Qmsplan(String title) {
        this.title = title;
        commontags = new HashSet<>();
        qmsschedules = new ArrayList<>();
        this.user = OPDE.getLogin().getUser();
        this.state = QmsplanTools.STATE_ACTIVE;
        this.color = "000000"; // Back in black
        this.version = 0l;
    }

    public Collection<Commontags> getCommontags() {
        return commontags;
    }


    public Collection<Qmsplan2File> getAttachedFilesConnections() {
        return attachedFilesConnections;
    }

    @Override
    public boolean isActive() {
        return state == QmsplanTools.STATE_ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Qmsplan qmsplan = (Qmsplan) o;

        if (id != qmsplan.id) return false;
        if (state != qmsplan.state) return false;
        if (description != null ? !description.equals(qmsplan.description) : qmsplan.description != null) return false;
        if (title != null ? !title.equals(qmsplan.title) : qmsplan.title != null) return false;
        if (version != null ? !version.equals(qmsplan.version) : qmsplan.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) state;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
