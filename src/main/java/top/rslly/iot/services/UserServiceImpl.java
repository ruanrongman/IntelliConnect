package top.rslly.iot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.models.UserEntity;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<UserEntity> findAllByUsername(String username) {
        return userRepository.findAllByUsername(username);
    }

    @Override
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    @Override
    public UserEntity insert(UserEntity l1) {
        l1.setPassword("{bcrypt}" + passwordEncoder.encode(l1.getPassword()));//adapt new version spring security
        return userRepository.save(l1);
    }
}
