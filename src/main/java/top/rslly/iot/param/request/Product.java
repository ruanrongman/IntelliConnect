package top.rslly.iot.param.request;

public class Product {
    private String productName;
    private String keyvalue;
    private int register;
    private int mqttUser;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getKeyvalue() {
        return keyvalue;
    }

    public void setKeyvalue(String keyvalue) {
        this.keyvalue = keyvalue;
    }

    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public int getMqttUser() {
        return mqttUser;
    }

    public void setMqttUser(int mqttUser) {
        this.mqttUser = mqttUser;
    }
}
