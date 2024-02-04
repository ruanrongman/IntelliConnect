package top.rslly.iot.services;

import top.rslly.iot.param.request.WxUser;
import top.rslly.iot.utility.result.JsonResult;

import java.io.IOException;

public interface WxUserService {
    JsonResult<?> wxLogin(WxUser wxUser) throws IOException;
    JsonResult<?> wxRegister(WxUser wxUser) throws IOException;
}
