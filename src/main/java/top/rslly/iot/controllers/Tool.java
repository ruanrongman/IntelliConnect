package top.rslly.iot.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.param.request.ReadData;
import top.rslly.iot.services.DataServiceImpl;
import top.rslly.iot.services.HardWareServiceImpl;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.RuntimeMessage;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultTool;

@RestController
@RequestMapping(value = "/api/v2")
public class Tool {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private DataServiceImpl dataService;
    @Autowired
    private HardWareServiceImpl hardWareService;

    @Operation(summary = "用于获取平台运行环境信息", description = "单位为百分比")
    @RequestMapping(value = "/machineMessage", method = RequestMethod.GET)
    public JsonResult<?> machineMessage() {
        return ResultTool.success(RuntimeMessage.getMessage());
    }

    @RequestMapping(value = "/control", method = RequestMethod.POST)
    public JsonResult<?> control(@RequestBody ControlParam controlParam) throws MqttException {

        return hardWareService.control(controlParam);
    }

    @Operation(summary = "用于获取物联网一段时间的设备数据", description = "使用两个毫秒时间戳")
    @RequestMapping(value = "/readData", method = RequestMethod.POST)
    public JsonResult<?> readData(@RequestBody ReadData readData) {
        return dataService.findAllByTimeBetweenAndDeviceName(readData.getTime1(), readData.getTime2(), readData.getName());
    }
}
