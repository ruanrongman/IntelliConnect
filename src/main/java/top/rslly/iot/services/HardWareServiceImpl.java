package top.rslly.iot.services;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductDataRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.transfer.mqtt.MqttConnectionUtils;
import top.rslly.iot.utility.JsonCreate;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HardWareServiceImpl implements HardWareService{
    @Resource
    private ProductDataRepository productDataRepository;
    @Resource
    private ProductDeviceRepository productDeviceRepository;

    @Override
    public JsonResult<?> control(ControlParam controlParam) throws MqttException {
        var deviceEntityList = productDeviceRepository.findAllByName(controlParam.getName());
        if(deviceEntityList.isEmpty()){
            return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        int modelId = deviceEntityList.get(0).getModelId();
        var productDataEntities=productDataRepository.findAllByModelId(modelId);
        if(productDataEntities.isEmpty()){
            return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        List<String> productDataKey=new ArrayList<>();
        Map<String,String> productTypeMap = new HashMap<>();
        List<String> productType = new ArrayList<>();
        for (var s:productDataEntities){
            if(s.getrRw()==1) {
                productDataKey.add(s.getJsonKey());
                productTypeMap.put(s.getJsonKey(), s.getType());
            }
        }
        if(!productDataKey.containsAll(controlParam.getKey())){
            return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        for(var s: controlParam.getKey()){
            productType.add(productTypeMap.get(s));
        }
        StringBuffer res = null;
        try {
            res = JsonCreate.create(controlParam.getKey(), controlParam.getValue(),productType);
        } catch (IOException e) {
            return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        MqttConnectionUtils.publish("/oc/devices/" + controlParam.getName() + "/sys/" +
                "properties/update", res.toString(), 0);
        return ResultTool.success();
    }
}
