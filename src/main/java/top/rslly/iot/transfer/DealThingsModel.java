package top.rslly.iot.transfer;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.DataEntity;
import top.rslly.iot.services.DataServiceImpl;
import top.rslly.iot.services.ProductDataServiceImpl;
import top.rslly.iot.services.ProductDeviceServiceImpl;

import java.util.UUID;

@Component
public class DealThingsModel {
    @Autowired
    private ProductDeviceServiceImpl productDeviceService;
    @Autowired
    private ProductDataServiceImpl productDataService;
    @Autowired
    private DataServiceImpl dataService;

    @Async("taskExecutor")
    public void deal(String clientId, long time, String message) {
        String characteristic = UUID.randomUUID().toString();
        var deviceEntityList = productDeviceService.findAllByClientId(clientId);
        if(deviceEntityList.isEmpty()) return;
        int modelId = deviceEntityList.get(0).getModelId();
        var productDataEntities = productDataService.findAllByModelId(modelId);
        var mes = JSON.parseObject(message);
        for (var s : productDataEntities) {
            var result = mes.get(s.getJsonKey());
            if (result == null) continue;//Automatically ignore values
            DataEntity dataEntity = new DataEntity();
            dataEntity.setCharacteristic(characteristic);
            dataEntity.setJsonKey(s.getJsonKey());
            dataEntity.setValue(result.toString());
            dataEntity.setDeviceId(deviceEntityList.get(0).getId());
            dataEntity.setTime(time);
            dataService.insert(dataEntity);
        }
    }
}
