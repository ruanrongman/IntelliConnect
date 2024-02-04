package top.rslly.iot.services;

import org.springframework.stereotype.Service;
import top.rslly.iot.dao.MqttUserRepository;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;

@Service
public class MqttUserServiceImpl implements MqttUserService{

    @Resource
    private MqttUserRepository mqttUserRepository;


    @Override
    public JsonResult<?> getMqttUser() {
        var result = mqttUserRepository.findAll();
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }
}
