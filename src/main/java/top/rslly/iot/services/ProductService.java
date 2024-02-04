package top.rslly.iot.services;


import top.rslly.iot.param.request.Product;
import top.rslly.iot.utility.result.JsonResult;

public interface ProductService {

    JsonResult<?> getProduct();
    JsonResult<?> postProduct(Product product);
    JsonResult<?> deleteProduct(int id);
}
