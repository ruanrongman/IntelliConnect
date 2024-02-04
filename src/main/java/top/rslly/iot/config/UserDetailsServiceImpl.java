package top.rslly.iot.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {
    /*@Autowired
    private PasswordEncoder passwordEncoder;*/

    @Autowired
    private UserService userService;

    private Logger LOG =  Logger.getLogger(String.valueOf(UserDetailsServiceImpl.class));

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        if (s == null || "".equals(s))
        {
            throw new RuntimeException("用户不能为空");
        }
        var user =  userService.findAllByUsername(s);
        if (user.isEmpty())
        {
            throw new RuntimeException("用户不存在");
        }
       // System.out.println(user.get(0));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_"+user.get(0).getRole()));

        String pwd = user.get(0).getPassword();
        //String cryptPwd = BCrypt.hashpw(pwd, BCrypt.gensalt());
        //String cryptPwd = passwordEncoder.encode(pwd);

        //LOG.info("加密后的密码为: {}"+cryptPwd);



        return new org.springframework.security.core.userdetails.User(s,pwd,authorities);
    }
}