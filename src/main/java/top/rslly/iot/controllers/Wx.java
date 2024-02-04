package top.rslly.iot.controllers;

import com.alibaba.fastjson.JSON;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.WxUser;
import top.rslly.iot.services.WxUserServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.io.IOException;
import java.util.List;

@RestController
public class Wx {

    @Autowired
    private WxUserServiceImpl wxUserService;
    @RequestMapping(value = "/wxLogin", method = RequestMethod.POST)
    public JsonResult<?> wxLogin(@RequestBody WxUser wxUser) throws IOException {
       return wxUserService.wxLogin(wxUser);
    }
    @RequestMapping(value = "/wxRegister",method = RequestMethod.POST)
    public JsonResult<?> wxRegister(@RequestBody WxUser wxUser) throws IOException {
        return wxUserService.wxRegister(wxUser);
    }

}
