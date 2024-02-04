package top.rslly.iot.param.request;

public class ProductData {
    private String jsonKey;
    private int modelId;
    private String description;
    private int rRw;
    private String type;

    public String getJsonKey() {
        return jsonKey;
    }

    public void setJsonKey(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getrRw() {
        return rRw;
    }

    public void setrRw(int rRw) {
        this.rRw = rRw;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
