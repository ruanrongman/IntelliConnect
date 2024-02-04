package top.rslly.iot.services;



import top.rslly.iot.models.UserEntity;

import java.util.List;

public interface UserService {
    List<UserEntity> findAllByUsername(String username);
    List<UserEntity> findAll();
    UserEntity insert(UserEntity u1);
}
