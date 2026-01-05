package top.rslly.iot.services.knowledgeGraphic;

import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;
import top.rslly.iot.param.request.KnowledgeGraphicNode;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface KnowledgeGraphicService {

    JsonResult<?> getKnowledgeGraphic(KnowledgeGraphicNodeEntity rootNode, int maxDepth);

    JsonResult<?> addNode(KnowledgeGraphicNodeEntity node);

    JsonResult<?> addNode(KnowledgeGraphicNode node);

    JsonResult<?> addNode(String name, String des, int id);

    JsonResult<?> getNode(String name);

    List<KnowledgeGraphicNodeEntity> getNodesById(long id);

    JsonResult<?> getNodeRelations(String name);

    JsonResult<?> getNodeRelations(long id);

    JsonResult<?> addAttribute(String name, long belong);

    JsonResult<?> addAttributes(List<String> attributes, long belong);
}
