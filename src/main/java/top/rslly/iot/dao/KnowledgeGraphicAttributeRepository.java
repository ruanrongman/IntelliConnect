package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;

import java.util.List;

public interface KnowledgeGraphicAttributeRepository extends JpaRepository<KnowledgeGraphicAttributeEntity, Long> {
    List<KnowledgeGraphicAttributeEntity> findByBelong(long belongId);

    void deleteByBelong(long belongId);

    KnowledgeGraphicAttributeEntity getByName(String name);

    List<KnowledgeGraphicAttributeEntity> getAllByBelong(long id);
}
