package top.rslly.iot.models;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "product", schema = "cwliot1.8", catalog = "")
public class ProductEntity {
    private int id;
    private String productName;
    private String keyvalue;
    private int register;
    private int mqttUser;

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
    @Column(name = "product_name")
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Basic
    @Column(name = "keyvalue")
    public String getKeyvalue() {
        return keyvalue;
    }

    public void setKeyvalue(String keyvalue) {
        this.keyvalue = keyvalue;
    }

    @Basic
    @Column(name = "register")
    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    @Basic
    @Column(name = "mqtt_user")
    public int getMqttUser() {
        return mqttUser;
    }

    public void setMqttUser(int mqttUser) {
        this.mqttUser = mqttUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductEntity that = (ProductEntity) o;
        return id == that.id && register == that.register && mqttUser == that.mqttUser && Objects.equals(productName, that.productName) && Objects.equals(keyvalue, that.keyvalue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productName, keyvalue, register, mqttUser);
    }
}
