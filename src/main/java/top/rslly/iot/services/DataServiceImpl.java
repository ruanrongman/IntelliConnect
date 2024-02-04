package top.rslly.iot.services;

import org.springframework.stereotype.Service;
import top.rslly.iot.dao.DataRepository;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.models.DataEntity;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DataServiceImpl implements DataService {

    @Resource
    private DataRepository dataRepository;
    @Resource
    private ProductDeviceRepository deviceRepository;

    @Override
    public DataEntity insert(DataEntity dataEntity) {
        return dataRepository.save(dataEntity);
    }

    @Override
    public JsonResult<?> findAllByTimeBetweenAndDeviceName(long time, long time2, String name) {
        var productDeviceEntities=deviceRepository.findAllByName(name);
        if(productDeviceEntities.isEmpty()){
            return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        }
        int deviceId = productDeviceEntities.get(0).getId();
        var res =dataRepository.findAllByTimeBetweenAndDeviceId(time,time2,deviceId);
        if (res.isEmpty()) return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        return ResultTool.success(res);
    }
}
