package top.rslly.iot.services;

import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.param.request.ProductData;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface ProductDataService {
    List<ProductDataEntity> findAllByModelId(int modelId);
    JsonResult<?> getProductData();
    JsonResult<?> postProductData(ProductData productData);
    JsonResult<?> deleteProductData(int id);
}
