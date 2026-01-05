package top.rslly.iot.models;

import lombok.Data;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Entity
@Table(name="knowledge_graphic_node", schema = "cwliot1.8", catalog = "")
public class KnowledgeGraphicNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="name")
    private String name;

    @Column(name="des")
    @Comment("Description of this node")
    private String des;

    @Column(name="product_uid")
    @Comment("Which user this node belongs to")
    private int productUid;

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

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public int getProductUid() {
        return productUid;
    }

    public void setProductUid(int productUid) {
        this.productUid = productUid;
    }
}
