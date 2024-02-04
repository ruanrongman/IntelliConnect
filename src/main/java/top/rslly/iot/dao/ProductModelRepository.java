package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.ProductModelEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface ProductModelRepository extends JpaRepository<ProductModelEntity,Long> {
      List<ProductModelEntity> findAllByProductId(int productId);
      List<ProductModelEntity> findAllByType(String type);
      List<ProductModelEntity> findAllById(int id);

      @Transactional
      List<ProductModelEntity> deleteById(int id);
}
