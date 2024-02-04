package top.rslly.iot;

import net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import top.rslly.iot.utility.SpringBeanUtils;


@SpringBootApplication(exclude = {GrpcServerSecurityAutoConfiguration.class})
@EnableAsync
public class DemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext=SpringApplication.run(DemoApplication.class, args);
        SpringBeanUtils.setApplicationContext(applicationContext);
    }

}
