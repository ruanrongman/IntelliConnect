package top.rslly.iot.services;

import org.springframework.beans.BeanUtils;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductDeviceRepository;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ProductDevice;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ProductDeviceServiceImpl implements ProductDeviceService{

    @Resource
    private ProductDeviceRepository productDeviceRepository;
    @Resource
    private ProductModelRepository productModelRepository;

    @Override
    public List<ProductDeviceEntity> findAllBySubscribeTopic(String subscribeTopic) {
        return productDeviceRepository.findAllBySubscribeTopic(subscribeTopic);
    }

    @Override
    public List<ProductDeviceEntity> findAllByClientId(String clientId) {
        return productDeviceRepository.findAllByClientId(clientId);
    }

    @Override
    public List<ProductDeviceEntity> deleteById(int id) {
        return productDeviceRepository.deleteById(id);
    }

    @Override
    public int updateOnlineByClientId(String online, String clientId) {
        return productDeviceRepository.updateOnlineByClientId(online,clientId);
    }

    @Override
    public JsonResult<?> getProductDevice() {
        var result = productDeviceRepository.findAll();
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }

    @Override
    public JsonResult<?> postProductDevice(ProductDevice productDevice) {
        ProductDeviceEntity productDeviceEntity = new ProductDeviceEntity();
        productDeviceEntity.setOnline("disconnected");
        BeanUtils.copyProperties(productDevice,productDeviceEntity);
        List<ProductModelEntity> result = productModelRepository.findAllById(productDeviceEntity.getModelId());
        List<ProductDeviceEntity> p1 = productDeviceRepository.findAllByClientId(productDeviceEntity.getClientId());
        List<ProductDeviceEntity> p2 = productDeviceRepository.findAllByName(productDeviceEntity.getName());
        if (result.isEmpty() || !p1.isEmpty()||!p2.isEmpty()) return ResultTool.fail(ResultCode.COMMON_FAIL);
        else if(productDeviceEntity.getName().matches(".*[\\u4E00-\\u9FA5]+.*"))return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        else {
            productDeviceEntity.setSubscribeTopic("/oc/devices/"+productDeviceEntity.getName()+"/sys/" +
                    "properties/report");
            ProductDeviceEntity productDeviceEntity1 = productDeviceRepository.save(productDeviceEntity);
            return ResultTool.success(productDeviceEntity1);
        }
    }

    @Override
    public JsonResult<?> deleteProductDevice(int id) {
        List<ProductDeviceEntity> result = productDeviceRepository.deleteById(id);
        if (result.isEmpty()) return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        else {
            return ResultTool.success(result);
        }
    }
}
