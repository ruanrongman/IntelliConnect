package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.DataEntity;

import java.util.List;

public interface DataRepository extends JpaRepository<DataEntity,Long>, JpaSpecificationExecutor<DataEntity>, QueryByExampleExecutor<DataEntity> {
    List<DataEntity> findAllByDeviceIdAndJsonKey(int deviceId, String jsonKey);


    List<DataEntity> findByCharacteristicAndDeviceId(String characteristic, int deviceId);

    List<DataEntity> findAllByTimeBetweenAndDeviceId(long time, long time2, int deviceId);

    @Query(value = "SELECT * FROM data WHERE device_id =?1 ORDER BY time DESC LIMIT 1",nativeQuery = true)
    List<DataEntity> findAllBySort(int deviceId);
    @Transactional
    List<DataEntity> deleteById(int id);
}
