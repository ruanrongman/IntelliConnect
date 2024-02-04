package top.rslly.iot.services;

import org.eclipse.paho.client.mqttv3.MqttException;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.utility.result.JsonResult;

public interface HardWareService {
    JsonResult<?> control(ControlParam controlParam) throws MqttException;
}
