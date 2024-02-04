package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.ProductEntity;


import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity,Long> {
    @Override
    List<ProductEntity> findAll();

    List<ProductEntity> findAllByKeyvalue(String keyvalue);
    List<ProductEntity> findAllById(int id);
    List<ProductEntity> findAllByProductName(String productName);

    @Transactional
    List<ProductEntity> deleteById(int id);

    @Transactional
    @Modifying
    @Query("update ProductEntity do set do.register=?1 where do.id=?2")
    int updateNameById(int register,int id);
}
