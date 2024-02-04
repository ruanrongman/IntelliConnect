package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductModelEntity;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ProductDataRepository extends JpaRepository<ProductDataEntity,Long> {
    List<ProductDataEntity> findAllByModelId(int modelId);
    List<ProductDataEntity> findAllByModelIdAndJsonKey(int modelId, String jsonKey);
    List<ProductDataEntity> findAllByType(String type);

    @Transactional
    List<ProductDataEntity> deleteById(int id);
}
