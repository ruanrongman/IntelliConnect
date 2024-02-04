package top.rslly.iot.utility;

import org.springframework.context.ApplicationContext;

public class SpringBeanUtils {
    private static ApplicationContext applicationContext;
    public static void setApplicationContext(ApplicationContext applicationContext){
        SpringBeanUtils.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    public static Object getBean(String BeanName){
        return applicationContext.getBean(BeanName);
    }
}
