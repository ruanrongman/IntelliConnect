package top.rslly.iot.param.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class KnowledgeGraphicAttribute {
    @NotBlank(message="属性名称不能为空！")
    public String name;
    @NotBlank(message="属性所属节点不能为空！")
    public long belong;
    @NotBlank(message="产品UID不能为空")
    public int productUid;
}
