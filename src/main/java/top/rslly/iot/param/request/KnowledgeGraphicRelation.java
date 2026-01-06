package top.rslly.iot.param.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class KnowledgeGraphicRelation {
    @NotBlank(message = "关系描述不能为空！")
    public String des;
    @NotBlank(message = "关系源不能为空！")
    public long from;
    @NotBlank(message = "关系目标不能为空！")
    public long to;
    @NotBlank(message = "产品UID不能为空！")
    public int productUid;
}
