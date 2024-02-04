package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import top.rslly.iot.models.WxUserEntity;

import java.util.List;

public interface WxUserRepository extends JpaRepository<WxUserEntity,Long> {
    List<WxUserEntity > findAllByOpenid(String openid);
}
