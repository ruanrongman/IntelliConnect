package top.rslly.iot.models;

import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Entity
@Table(name="knowledge_graphic_attribute", schema="cwloit1.8")
public class KnowledgeGraphicAttributeEntity {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name="name")
    private String name;

    @Column(name="belong")
    @Comment("Node id which this attribute belongs to")
    private long belong;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getBelong() {
        return belong;
    }

    public void setBelong(long belong) {
        this.belong = belong;
    }
}
