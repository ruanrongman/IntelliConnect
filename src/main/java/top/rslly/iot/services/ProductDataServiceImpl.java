package top.rslly.iot.services;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.ProductDataRepository;
import top.rslly.iot.dao.ProductModelRepository;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.request.ProductData;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Service
public class ProductDataServiceImpl implements ProductDataService{

    @Resource
    private ProductDataRepository productDataRepository;
    @Resource
    private ProductModelRepository productModelRepository;

    @Override
    public List<ProductDataEntity> findAllByModelId(int modelId) {
        return productDataRepository.findAllByModelId(modelId);
    }

    @Override
    public JsonResult<?> getProductData() {
        var result = productDataRepository.findAll();
        if (result.isEmpty()) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        } else return ResultTool.success(result);
    }

    @Override
    public JsonResult<?> postProductData(ProductData productData) {
        ProductDataEntity productDataEntity = new ProductDataEntity();
        BeanUtils.copyProperties(productData,productDataEntity);
        List<ProductModelEntity> result = productModelRepository.findAllById(productDataEntity.getModelId());
        List<ProductDataEntity> p1 = productDataRepository.findAllByModelIdAndJsonKey(productDataEntity.getModelId(),productDataEntity.getJsonKey());
        //List<ProductDataEntity> p2 = productDataRepository.findAllByType(productData.getType());
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("string", 1);
        hashMap.put("int", 2);
        hashMap.put("float", 3);
        var p2 = hashMap.get(productDataEntity.getType());
        boolean isPermit = productDataEntity.getrRw()==0||productDataEntity.getrRw()==1;
        if(result.isEmpty()||!p1.isEmpty()||p2==null||!isPermit)return ResultTool.fail(ResultCode.COMMON_FAIL);
        else{
            ProductDataEntity productDataEntity1=productDataRepository.save(productDataEntity);
            return ResultTool.success(productDataEntity1);
        }
    }

    @Override
    public JsonResult<?> deleteProductData(int id) {
        List<ProductDataEntity> result = productDataRepository.deleteById(id);
        if (result.isEmpty()) return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        else {
            return ResultTool.success(result);
        }
    }
}
