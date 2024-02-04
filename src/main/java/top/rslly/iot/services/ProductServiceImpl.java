package top.rslly.iot.services;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.MqttUserRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.MqttUserEntity;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.param.request.Product;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;


import javax.annotation.Resource;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    @Resource
    private ProductRepository productRepository;
    @Resource
    private MqttUserRepository mqttUserRepository;

    @Override
    public JsonResult<?> getProduct() {
        var result = productRepository.findAll();
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }

    @Override
    public JsonResult<?> postProduct(Product product) {
        ProductEntity productEntity = new ProductEntity();
        BeanUtils.copyProperties(product,productEntity);
        List<MqttUserEntity> result = mqttUserRepository.findALLById(productEntity.getMqttUser());
        List<ProductEntity> p1 = productRepository.findAllByProductName(productEntity.getProductName());
        if (result.isEmpty() || !p1.isEmpty()) return ResultTool.fail(ResultCode.COMMON_FAIL);
        else {
            ProductEntity productEntity1 = productRepository.save(productEntity);
            return ResultTool.success(productEntity1);
        }
    }

    @Override
    public JsonResult<?> deleteProduct(int id) {
        List<ProductEntity> result = productRepository.deleteById(id);
        if (result.isEmpty()) return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        else {
            return ResultTool.success(result);
        }
    }
}
