package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;

import java.util.List;

public interface KnowledgeGraphicNodeRepository extends JpaRepository<KnowledgeGraphicNodeEntity, Long> {
    public List<KnowledgeGraphicNodeEntity> findAllById(long id);

    public KnowledgeGraphicNodeEntity findByName(String name);

    List<KnowledgeGraphicNodeEntity> findAllByProductUid(int id);

    void deleteAllByProductUid(int id);
}
