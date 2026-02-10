package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.KnowledgeGraphicSearchCountEntity;

public interface KnowledgeGraphicSearchCountRepository extends JpaRepository<KnowledgeGraphicSearchCountEntity, Long> {
    KnowledgeGraphicSearchCountEntity getTopByProductId(int productId);

    void deleteAllByProductId(int productId);
}
