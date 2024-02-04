package top.rslly.iot.services;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ProductModel;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ProductModelServiceImpl implements ProductModelService{

    @Resource
    private ProductModelRepository productModelRepository;
    @Resource
    private ProductRepository productRepository;

    @Override
    public JsonResult<?> getProductModel() {
        var result = productModelRepository.findAll();
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }

    @Override
    public JsonResult<?> getProductModel(int productId) {
        var result = productModelRepository.findAllByProductId(productId);
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }

    @Override
    public JsonResult<?> postProductModel(ProductModel productModel) {
        ProductModelEntity productModelEntity = new ProductModelEntity();
        BeanUtils.copyProperties(productModel,productModelEntity);
        List<ProductEntity> result = productRepository.findAllById(productModel.getProductId());
        List<ProductModelEntity> p1 = productModelRepository.findAllByType(productModel.getType());
        if (result.isEmpty() || !p1.isEmpty()) return ResultTool.fail(ResultCode.COMMON_FAIL);
        else {
            ProductModelEntity productModelEntity1 = productModelRepository.save(productModelEntity);
            return ResultTool.success(productModelEntity1);
        }
    }

    @Override
    public JsonResult<?> deleteProductModel(int id) {
        List<ProductModelEntity> result = productModelRepository.deleteById(id);
        if (result.isEmpty()) return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        else {
            return ResultTool.success(result);
        }
    }
}
