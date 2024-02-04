package top.rslly.iot.utility.wx;

import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.RedisUtil;

import java.io.IOException;

@Component
public class DealWx {
    @Autowired
    private HttpRequestUtils httpRequestUtils;
    @Autowired
    private RedisUtil redisUtil;

    public String getAccessToken(String appid, String appSecret) throws IOException {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + appSecret;
        Response s = httpRequestUtils.httpGet(url);
        return s.body().string();
    }
    public String GetOpenid(String code, String microappid, String microappsecret) throws IOException {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+microappid+"&secret="+microappsecret+"&js_code="+code+"&grant_type=authorization_code";
        String res=httpRequestUtils.httpGet(url).body().string();
        System.out.println(res);
        return res;
    }
    public void SendContent(String openid,String content,String microappid) throws IOException {
        String token= (String) redisUtil.get(microappid);
        String url="https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token="+token;
        String jsonstr="{\n" +
                "  \"touser\":\""+openid+"\",\n" +
                "  \"msgtype\":\"text\",\n" +
                "  \"text\":\n" +
                "  {\n" +
                "    \"content\":\""+content+"\"\n" +
                "  }\n" +
                "}";
        httpRequestUtils.asyncPostByJson(url,jsonstr);
    }

}
