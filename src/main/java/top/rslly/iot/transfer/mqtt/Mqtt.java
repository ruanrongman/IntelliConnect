package top.rslly.iot.transfer.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
public class Mqtt implements ApplicationRunner {


    private static final Logger LOG = LoggerFactory.getLogger(Mqtt.class);
    private String Host;
    private String Username;
    private String Password;

    @Value("${mqtt.Host}")
    public void setHost(String host) {
        this.Host = host;
        LOG.info("用户名" + host);
    }

    @Value("${mqtt.username}")
    public void setUsername(String username) {
        this.Username = username;
        LOG.info("用户名" + username);
    }

    @Value("${mqtt.password}")
    public void setPassword(String password) {
        this.Password = password;
        LOG.info("密码" + password);
    }

    @Override
    @Order(1)
    public void run(ApplicationArguments args) throws Exception {
        MqttConnectionUtils.start(Host, Username, Password);
    }
}
