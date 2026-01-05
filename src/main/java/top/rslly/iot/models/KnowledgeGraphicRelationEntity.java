package top.rslly.iot.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="knowledge_graphic_relation", schema = "cwliot1.8")
public class KnowledgeGraphicRelationEntity {
    @Id
    @Column(name="id")
    private long id;

    @Column(name="from")
    private long from;

    @Column(name="to")
    private long to;

    @Column(name="des")
    private String des;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}
