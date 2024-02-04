package top.rslly.iot.transfer.mqtt;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class MqttConnectionUtils {

    private static MqttClient client;

    private static MqttConnectOptions connectOptions;

    private static String clientId;

   // private static String Host;

   // private static String Username;

   // private static String Password;

    private static final Logger LOG = LoggerFactory.getLogger(MqttConnectionUtils.class);




    /**
     * 发送数据
     */

    public static void publish(String topic,String content,int qos) throws MqttException {
        MqttMessage message=new MqttMessage(content.getBytes());
        message.setQos(qos);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        LOG.info("发送时间========"+df.format(new Date()));
        LOG.info(topic+"主题发送成功，内容:"+message);
        client.publish(topic,message);
    }

    /**
     * 接收数据
     */

    public static void start(String Host, String Username, String Password) throws MqttException {
        try {
            clientId = UUID.randomUUID().toString().trim().replaceAll("-", "");
            client = new MqttClient(Host,clientId);
            connectOptions=new MqttConnectOptions();
            connectOptions.setCleanSession(false);
            connectOptions.setUserName(Username);
            connectOptions.setPassword(Password.toCharArray());//密码            connectOptions.setConnectionTimeout(10);
            client.setTimeToWait(10000);
            client.connect(connectOptions);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //MqttTopic topic = client.getTopic(TOPIC);
        // setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
       // connectOptions.setWill(topic, "close".getBytes(), 2, true);

        LOG.info("WIFI版启动成功=================");
    }


    /**
     * mqtt重连
     */
    public static void reConnect() {
        while (true){
            try {
                if (null != client && !(client.isConnected())) {
                    Thread.sleep(1000);
                    clientId = UUID.randomUUID().toString().trim().replaceAll("-", "");
                    client.connect(connectOptions);
                    LOG.info("=======尝试重新连接==============");
                    break;
                }
            } catch (MqttException | InterruptedException e) {
                LOG.info("=======重新连接失败==============");
            }
        }

    }
}
