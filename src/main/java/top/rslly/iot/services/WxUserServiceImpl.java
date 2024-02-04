package top.rslly.iot.services;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.request.WxUser;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;
import top.rslly.iot.utility.wx.DealWx;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class WxUserServiceImpl implements WxUserService{
    @Resource
    private WxUserRepository wxUserRepository;
    @Autowired
    private DealWx dealWx;
    @Value("${wx.micro.appid}")
    private String microAppid;
    @Value("${wx.micro.appsecret}")
    private String microAppSecret;

    @Override
    public JsonResult<?> wxLogin(WxUser wxUser) throws IOException {
        String s = dealWx.GetOpenid(wxUser.getCode(),microAppid,microAppSecret);
        String openid = (String) JSON.parseObject(s).get("openid");
        //System.out.println(openid);
        List<WxUserEntity> user = wxUserRepository.findAllByOpenid(openid);
        if (user.isEmpty()) return ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
        return ResultTool.success(JwtTokenUtil.TOKEN_PREFIX + JwtTokenUtil.createToken(user.get(0).getName(), "ROLE_" + "wx_user"));
    }

    @Override
    public JsonResult<?> wxRegister(WxUser wxUser) throws IOException {
        String s = dealWx.GetOpenid(wxUser.getCode(),microAppid,microAppSecret);
        String openid = (String) JSON.parseObject(s).get("openid");
        List<WxUserEntity> user = wxUserRepository.findAllByOpenid(openid);
        if (!user.isEmpty()) return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
        WxUserEntity wxUserEntity = new WxUserEntity();
        wxUserEntity.setName(UUID.randomUUID().toString());
        wxUserEntity.setOpenid(openid);
        var res=wxUserRepository.save(wxUserEntity);
        return ResultTool.success(res);
    }
}
