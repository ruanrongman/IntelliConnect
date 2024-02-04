package top.rslly.iot.services;

import top.rslly.iot.param.request.ProductModel;
import top.rslly.iot.utility.result.JsonResult;

public interface ProductModelService {
    JsonResult<?> getProductModel();
    JsonResult<?> getProductModel(int productId);
    JsonResult<?> postProductModel(ProductModel productModel);
    JsonResult<?> deleteProductModel(int id);

}
