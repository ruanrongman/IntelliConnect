package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.MqttUserEntity;


import java.util.List;

public interface MqttUserRepository extends JpaRepository<MqttUserEntity,Long> {

    List<MqttUserEntity> findALLById(int id);


}
