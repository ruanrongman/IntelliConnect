package top.rslly.iot.error;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class MainsiteErrorController implements ErrorController {


    private final String ERROR_PATH ="/error";


    @RequestMapping(value =ERROR_PATH)
    public JsonResult<?> handleError(HttpServletRequest request, HttpServletResponse response) {
        int code = response.getStatus();
        if (code == 404) {
            return  ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        } else {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
    }

}
