package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;

import java.util.List;

public interface KnowledgeGraphicRelationRepository extends JpaRepository<KnowledgeGraphicRelationEntity, Long> {
    public List<KnowledgeGraphicRelationEntity> getAllByFrom(long fromId);

    public List<KnowledgeGraphicRelationEntity> getAllByTo(long toId);

    void deleteAllByFrom(long fromId);
}
