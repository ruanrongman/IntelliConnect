package top.rslly.iot.utility.wx;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.RedisUtil;

import java.io.IOException;

@Component
public class UpdateTask {

    @Value("${wx.micro.appid}")
    private String microappid;
    @Value("${wx.micro.appsecret}")
    private String microappsecret;
    @Value("${wx.debug}")
    private String debug;
    @Autowired
    private DealWx dealWx;
    @Autowired
    private RedisUtil redisUtil;
    private static final Logger LOG = LoggerFactory.getLogger(UpdateTask.class);

    @Scheduled(initialDelay = 1000, fixedDelay = 1000 * 3600)
    public void task() {
        //Timer timer = new Timer();
        //  timer.schedule(new TimerTask() {
        // public void run() {
        try {
            LOG.info(debug);
            if (debug.equals("false")) {
                String microtoken = (String) JSON.parseObject(dealWx.getAccessToken(microappid, microappsecret)).get("access_token");
                redisUtil.set(microappid, microtoken, 7200);
                //System.out.println(token);
                LOG.info("====获取到操作小程序token为" + microtoken + "====");
            } else LOG.info("调试模式，不获取token");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("11232");
    }
    //   }, 1000 , 1000*3600);
}

