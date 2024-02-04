package top.rslly.iot.services;

import top.rslly.iot.models.DataEntity;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface DataService {
    DataEntity insert(DataEntity dataEntity);
    JsonResult<?> findAllByTimeBetweenAndDeviceName(long time, long time2, String name);

}
