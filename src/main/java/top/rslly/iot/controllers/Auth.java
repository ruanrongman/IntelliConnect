package top.rslly.iot.controllers;


import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.param.request.Product;
import top.rslly.iot.param.request.ProductData;
import top.rslly.iot.param.request.ProductDevice;
import top.rslly.iot.param.request.ProductModel;
import top.rslly.iot.services.*;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultTool;

@RestController
@RequestMapping(value = "/api/v2")
public class Auth {

    @Autowired
    ProductServiceImpl productService;
    @Autowired
    ProductModelServiceImpl productModelService;
    @Autowired
    ProductDeviceServiceImpl productDeviceService;
    @Autowired
    ProductDataServiceImpl productDataService;
    @Autowired
    MqttUserServiceImpl mqttUserService;


    //获取所有mqtt用户信息
    @PreAuthorize("hasRole('ROLE_admin')")
    @RequestMapping(value = "/mqttUser", method = RequestMethod.GET)
    public JsonResult<?> MqttUser() {
        return mqttUserService.getMqttUser();
    }

    //添加产品信息，如产品密钥这些，后续版本将会添加更多细节。
    @Operation(summary = "获取产品信息", description = "比如产品密钥和注册状态")
    @RequestMapping(value = "/Product", method = RequestMethod.GET)
    public JsonResult<?> Product() {
        return productService.getProduct();
    }

    @PreAuthorize("hasRole('ROLE_admin')")
    @Operation(summary = "提交产品信息", description = "比如产品密钥和注册状态")
    @RequestMapping(value = "/Product", method = RequestMethod.POST)
    public JsonResult<?> Product(@RequestBody Product product) {
        return productService.postProduct(product);
    }

    @PreAuthorize("hasRole('ROLE_admin')")
    @Operation(summary = "删除产品信息（慎用）", description = "比如产品密钥和注册状态")
    @RequestMapping(value = "/Product", method = RequestMethod.DELETE)
    public JsonResult<?> Product(@RequestParam("id") int id) {
        return productService.deleteProduct(id);
    }

    @Operation(summary = "获取物模型", description = "获取所有产品的物模型")
    @RequestMapping(value = "/ProductModel", method = RequestMethod.GET)
    public JsonResult<?> ProductModel() {
        return productModelService.getProductModel();
    }

    @Operation(summary = "获取物模型", description = "获取对应产品的物模型")
    @RequestMapping(value = "/ProductModelByproductId", method = RequestMethod.GET)
    public JsonResult<?> ProductModelByproductId(@RequestParam("productId") int productId) {
        return productModelService.getProductModel(productId);
    }

    @Operation(summary = "创建物模型", description = "创建产品的物模型")
    @RequestMapping(value = "/ProductModel", method = RequestMethod.POST)
    public JsonResult<?> ProductModel(@RequestBody ProductModel productModel) {
        return productModelService.postProductModel(productModel);
    }

    @Operation(summary = "删除物模型", description = "删除产品的物模型")
    @RequestMapping(value = "/ProductModel", method = RequestMethod.DELETE)
    public JsonResult<?> ProductModel(@RequestParam("id") int id) {
        return productModelService.deleteProductModel(id);
    }

    @Operation(summary = "获取设备", description = "获取物模型的设备")
    @RequestMapping(value = "/ProductDevice", method = RequestMethod.GET)
    public JsonResult<?> ProductDevice() {
        return productDeviceService.getProductDevice();
    }

    @Operation(summary = "创建设备", description = "创建物模型的设备")
    @RequestMapping(value = "/ProductDevice", method = RequestMethod.POST)
    public JsonResult<?> ProductDevice(@RequestBody ProductDevice productDevice) {
        return productDeviceService.postProductDevice(productDevice);
    }

    @Operation(summary = "删除设备", description = "删除物模型的设备")
    @RequestMapping(value = "/ProductDevice", method = RequestMethod.DELETE)
    public JsonResult<?> ProductDevice(@RequestParam("id") int id) {
        return productDeviceService.deleteProductDevice(id);
    }
    @Operation(summary = "获取属性", description = "获取物模型的属性")
    @RequestMapping(value = "/ProductData", method = RequestMethod.GET)
    public JsonResult<?> ProductData() {
        return productDataService.getProductData();
    }
    @Operation(summary = "创建属性", description = "创建物模型的属性")
    @RequestMapping(value = "/ProductData", method = RequestMethod.POST)
    public JsonResult<?> ProductData(@RequestBody ProductData productData) {
        return productDataService.postProductData(productData);
    }
    @Operation(summary = "删除属性", description = "删除物模型的属性")
    @RequestMapping(value = "/ProductData", method = RequestMethod.DELETE)
    public JsonResult<?> ProductData(@RequestParam("id") int id) {
        return productDataService.deleteProductData(id);
    }



}
