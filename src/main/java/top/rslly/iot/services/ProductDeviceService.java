package top.rslly.iot.services;

import top.rslly.iot.models.ProductDeviceEntity;
import top.rslly.iot.param.request.ProductDevice;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface ProductDeviceService {
    List<ProductDeviceEntity> findAllBySubscribeTopic(String subscribeTopic);
    List<ProductDeviceEntity> findAllByClientId(String clientId);
    List<ProductDeviceEntity> deleteById(int id);
    int updateOnlineByClientId(String online,String clientId);
    JsonResult<?> getProductDevice();
    JsonResult<?> postProductDevice(ProductDevice productDevice);
    JsonResult<?> deleteProductDevice(int id);

}
