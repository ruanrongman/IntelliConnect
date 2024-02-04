package top.rslly.iot.utility;


import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Component
public class HttpRequestUtils {

    private final OkHttpClient okHttpClient;

    public HttpRequestUtils() {
        okHttpClient = new OkHttpClient();
    }

    public Response httpGet(String url) throws IOException {


        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);

        return call.execute();
    }
    public Response httpGetAuthentication(String name, String password,String url) throws IOException {
        String credential = Credentials.basic(name, password);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build();
        Call call = okHttpClient.newCall(request);

        return call.execute();

    }
    public Response httpDeleteAuthentication(String name, String password,String url,Map<String,String> param) throws IOException {
        String credential = Credentials.basic(name, password);
        StringBuilder requestUrl=new StringBuilder(url);
        if(param!=null&&(param.size()>0)) {
            requestUrl.append("/?");

            Iterator<Map.Entry<String, String>> iterator = param.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String,String> entry=iterator.next();
                requestUrl.append(entry.getKey());
                requestUrl.append("=");
                requestUrl.append(entry.getValue());
                if(iterator.hasNext())requestUrl.append("&");
            }
        }
       // System.out.println(requestUrl);
        Request request = new Request.Builder()
                .delete()
                .url(requestUrl.toString())
                .header("Authorization", credential)
                .build();
        Call call = okHttpClient.newCall(request);

        return call.execute();
    }

    public Response httpPost(String url, String text) throws IOException {

        // RequestBody body =setRequestBody(bodyParam);
        RequestBody body = new FormBody.Builder()
                .add("text", text)

                .build();
        Request.Builder requestBuilder = new Request.Builder();

        Request request = requestBuilder
                .post(body)
                .url(url)
                .build();

        Call call = okHttpClient.newCall(request);

        return call.execute();


    }

    public String createHttpsPostByjson(String url, String json) throws IOException {
        // StringBuilder buffer = new StringBuilder("");

        OkHttpClient build = okHttpClient.newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody requestBody = RequestBody.create(mediaType, json);
        ;

        Request request = new Request.Builder().url(url).post(requestBody).build();

        Call call = build.newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    public void asyncPostByJson(String url, String json) throws IOException {
        OkHttpClient build = okHttpClient.newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody requestBody = RequestBody.create(mediaType, json);
        ;

        Request request = new Request.Builder().url(url).post(requestBody).build();
        Call call = build.newCall(request);
        call.enqueue(new Callback() {//4.回调方法
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();//5.获得网络数据
                System.out.println(result);
            }
        });

    }

    private RequestBody setRequestBody(Map<String, String> bodyParams) {
        RequestBody body;
        FormBody.Builder formEncodingBuilder = new FormBody.Builder();
        if (bodyParams != null) {
            Iterator<String> iterator = bodyParams.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                formEncodingBuilder.add(key, bodyParams.get(key));
            }
        }
        body = formEncodingBuilder.build();
        return body;
    }
}
