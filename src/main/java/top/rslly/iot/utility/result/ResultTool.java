package top.rslly.iot.utility.result;


/**
 * @Author: Hutengfei
 * @Description:
 * @Date Create in 2019/7/22 19:52
 */
public class ResultTool {
    public static JsonResult<?> success() {
        return new JsonResult<>(true);
    }

    public static <T> JsonResult<T> success(T data) {
        return new JsonResult<>(true, data);
    }

    public static JsonResult<?> fail() {
        return new JsonResult<>(false);
    }

    public static JsonResult<?> fail(ResultCode resultEnum) {
        return new JsonResult<>(false, resultEnum);
    }

}