package top.rslly.iot.models;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "product_data", schema = "cwliot1.8", catalog = "")
public class ProductDataEntity {
    private int id;
    private String jsonKey;
    private int modelId;
    private String description;
    private int rRw;
    private String type;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "json_key")
    public String getJsonKey() {
        return jsonKey;
    }

    public void setJsonKey(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    @Basic
    @Column(name = "model_id")
    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    @Basic
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "r_rw")
    public int getrRw() {
        return rRw;
    }

    public void setrRw(int rRw) {
        this.rRw = rRw;
    }

    @Basic
    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDataEntity that = (ProductDataEntity) o;
        return id == that.id && modelId == that.modelId && rRw == that.rRw && Objects.equals(jsonKey, that.jsonKey) && Objects.equals(description, that.description) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jsonKey, modelId, description, rRw, type);
    }
}
