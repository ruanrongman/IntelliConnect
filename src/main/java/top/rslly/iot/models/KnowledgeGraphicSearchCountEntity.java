package top.rslly.iot.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name="knowledge_graphic_search_count", schema = "cwloit1.8")
public class KnowledgeGraphicSearchCountEntity {
    @Id
    @Column(name="id")
    private long id;

    @Column(name="product_id")
    private int productId;

    @Column(name="r_count")
    private int count;
}
