package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.models.ProductModelEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface ProductDeviceRepository extends JpaRepository<ProductDeviceEntity,Long> {

    List<ProductDeviceEntity> findAllBySubscribeTopic(String subscribeTopic);
    List<ProductDeviceEntity> findAllByClientId(String clientId);
    List<ProductDeviceEntity> findAllById(int id);
    List<ProductDeviceEntity> findAllByName(String name);

    @Transactional
    List<ProductDeviceEntity> deleteById(int id);

    @Transactional
    @Modifying
    @Query("update ProductDeviceEntity do set do.online=?1 where do.clientId=?2")
    int updateOnlineByClientId(String online,String clientId);
}
