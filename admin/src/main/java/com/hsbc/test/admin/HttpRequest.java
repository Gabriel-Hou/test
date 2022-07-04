package com.hsbc.test.admin;

import com.alibaba.fastjson.JSONObject;
import com.hsbc.test.common.infrastructure.utils.string.StringUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class HttpRequest {

    //参数携带的方式
    public static final String JSON_CARRY_TYPE = "json";
    public static final String URL_CARRY_TYPE = "url";

    /**
     * GET请求 忽略证书
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @return
     */
    public static String sendGetRequestNOTSSL(String url, Map<String, String> parameter) {
        try {
            //4.3以后的版本是这样的
            HttpClient client = new DefaultHttpClient();
            client = wrapClient(client);
            if (!CollectionUtils.isEmpty(parameter)) {
                String requestParam = fromMapToUrlParam(parameter);
                url = url + "?" + requestParam;
            }
            HttpGet httget = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(60 * 1000)
                    .setConnectTimeout(60 * 1000)
                    .build();
            httget.setConfig(requestConfig);

            HttpResponse response = client.execute(httget);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            return result;
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return "";
    }


    /**
     * 发送请求，地址携带参数
     *
     * @param urlString     请求url
     * @param requestMethod "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
     * @param map           参数组装
     * @return
     */
    public static String sendRequestToUrlCarryParameter(String urlString, String requestMethod, Map<String, String> map) {
        StringBuilder res = new StringBuilder();
        String requestUrl = urlString;
        BufferedReader in = null;
        try {
            if (map != null) {
                String requestParam = fromMapToUrlParam(map);
                requestUrl = requestUrl + "?" + requestParam;
            }

            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setDoOutput(true);
            conn.setConnectTimeout(60 * 1000); //单位：毫秒
            conn.setReadTimeout(60 * 1000);//单位：毫秒
            conn.setRequestMethod(requestMethod);
            conn.connect();

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                res.append(line);
            }
        } catch (Exception e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    log.error("关闭httpClient出现error：" + e.getMessage(), e);
                }
            }
        }
        return res.toString();
    }

    /**
     * POST请求
     *
     * @param url       URL
     * @param param     提交参数
     * @param carryType 携带参数的类型 json，url
     * @return
     */
    public static JSONObject sendPostRequest(String url, String param, String carryType) {
        JSONObject jsonObject = null;
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpost = getHttpPost(url, param, carryType);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(60 * 1000)
                    .setConnectTimeout(60 * 1000)
                    .build();
            httpost.setConfig(requestConfig);
            HttpResponse response = client.execute(httpost);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (StringUtil.isNotBlank(result)) {
                jsonObject = JSONObject.parseObject(result);
            }
        } catch (UnsupportedCharsetException | ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return jsonObject;
    }

    /**
     * POST请求(application/x-www-form-urlencoded的格式的数据类型)
     *
     * @param url   URL
     * @param param 提交参数
     * @return
     */
    public static String sendPostRequestByFormType(String url, Map<String, String> param, Map<String, String> header) {
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = getHttpPost(url, null, JSON_CARRY_TYPE);
            if (httpPost == null) {
                return null;
            }
            if (!CollectionUtils.isEmpty(header)) {
                header.forEach(httpPost::setHeader);
            }
            if (!CollectionUtils.isEmpty(param)) {
                List<NameValuePair> reqeustData = new ArrayList<>(param.size());
                for (String key : param.keySet()) {
                    reqeustData.add(new BasicNameValuePair(key, param.get(key)));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(reqeustData, "UTF-8"));
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(120 * 1000)
                    .setConnectTimeout(120 * 1000)
                    .build();
            httpPost.setConfig(requestConfig);
            HttpResponse response = client.execute(httpPost);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (UnsupportedCharsetException | ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return null;
    }


    /**
     * POST请求, 没有超时，无限等待
     *
     * @param url       URL
     * @param param     提交参数
     * @param carryType 携带参数的类型 json，url
     * @return
     */
    public static JSONObject sendPostRequestNotTimeOut(String url, String param, String carryType) {
        JSONObject jsonObject = null;
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpost = getHttpPost(url, param, carryType);

            HttpResponse response = client.execute(httpost);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (StringUtil.isNotBlank(result)) {
                jsonObject = JSONObject.parseObject(result);
            }
        } catch (UnsupportedCharsetException | IOException | ParseException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return jsonObject;
    }


    /**
     * GET请求
     *
     * @param url
     * @return
     */
    public static JSONObject sendGetRequest(String url) {
        return sendGetRequest(url, null);
    }


    /**
     * GET请求
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @return
     */
    public static JSONObject sendGetRequest(String url, Map<String, String> parameter) {
        JSONObject jsonObject = null;

        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            if (!CollectionUtils.isEmpty(parameter)) {
                String requestParam = fromMapToUrlParam(parameter);
                url = url + "?" + requestParam;
            }
            HttpGet httget = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(60 * 1000)
                    .setConnectTimeout(60 * 1000)
                    .build();
            httget.setConfig(requestConfig);

            HttpResponse response = client.execute(httget);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (StringUtil.isNotBlank(result)) {
                jsonObject = JSONObject.parseObject(result);
            }
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return jsonObject;
    }

    /**
     * GET请求
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @return
     */
    public static String sendGetRequestToString(String url, Map<String, String> parameter) {
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            if (!CollectionUtils.isEmpty(parameter)) {
                String requestParam = fromMapToUrlParam(parameter);
                url = url + "?" + requestParam;
            }
            HttpGet httget = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(60 * 1000)
                    .setConnectTimeout(60 * 1000)
                    .build();
            httget.setConfig(requestConfig);

            HttpResponse response = client.execute(httget);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * GET请求，不超时
     *
     * @param url 请求地址
     * @return
     */
    public static JSONObject sendGetRequestNotTimeOut(String url) {
        return sendGetRequestNotTimeOut(url, null, null);
    }

    /**
     * GET请求，不超时
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @return
     */
    public static JSONObject sendGetRequestNotTimeOut(String url, Map<String, String> parameter) {
        return sendGetRequestNotTimeOut(url, parameter, null);
    }

    /**
     * GET请求，不超时
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @param header    请求头
     * @return
     */
    public static JSONObject sendGetRequestNotTimeOut(String url, Map<String, String> parameter, Map<String, String> header) {
        if (!CollectionUtils.isEmpty(parameter)) {
            String requestParam = fromMapToUrlParam(parameter);
            url = url + "?" + requestParam;
        }

        JSONObject jsonObject = null;
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httget = new HttpGet(url);
            if (!CollectionUtils.isEmpty(header)) {
                header.forEach(httget::setHeader);
            }

            HttpResponse response = client.execute(httget);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (StringUtil.isNotBlank(result)) {
                jsonObject = JSONObject.parseObject(result);
            }
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return jsonObject;
    }

    /**
     * GET请求，不超时
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @param header    请求头
     * @return
     */
    public static String sendGetRequestToStringNotTimeOut(String url, Map<String, String> parameter, Map<String, String> header) {
        if (!CollectionUtils.isEmpty(parameter)) {
            String requestParam = fromMapToUrlParam(parameter);
            url = url + "?" + requestParam;
        }

        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httget = new HttpGet(url);
            if (!CollectionUtils.isEmpty(header)) {
                header.forEach(httget::setHeader);
            }

            HttpResponse response = client.execute(httget);
            int code = response.getStatusLine().getStatusCode();
            log.info("GET请求 状态码：code == {}", code);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * GET请求，不超时
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @param header    请求头
     * @return
     */
    public static HttpResponse sendGetRequestToStringByRes(String url, Map<String, String> parameter, Map<String, String> header) {
        if (!CollectionUtils.isEmpty(parameter)) {
            String requestParam = fromMapToUrlParam(parameter);
            url = url + "?" + requestParam;
        }

        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(30 * 1000)
                    .setConnectTimeout(30 * 1000)
                    .build();
            httpGet.setConfig(requestConfig);
            if (!CollectionUtils.isEmpty(header)) {
                header.forEach(httpGet::setHeader);
            }
            return client.execute(httpGet);
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * GET请求，不超时
     *
     * @param url       请求地址
     * @param parameter 请求参数
     * @param header    请求头
     * @return
     */
    public static byte[] sendGetRequestNotTimeOutToByteResult(String url, Map<String, String> parameter, Map<String, String> header) {
        if (!CollectionUtils.isEmpty(parameter)) {
            String requestParam = fromMapToUrlParam(parameter);
            url = url + "?" + requestParam;
        }

        JSONObject jsonObject = null;
        try {
            //4.3以后的版本是这样的
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httget = new HttpGet(url);
            if (!CollectionUtils.isEmpty(header)) {
                header.forEach(httget::setHeader);
            }

            HttpResponse response = client.execute(httget);
            return EntityUtils.toByteArray(response.getEntity());
        } catch (ParseException | IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * 发送post xml数据
     *
     * @param url         地址
     * @param xml         xml数据
     * @param contentType 内容类型
     * @param soapAction
     * @return
     */
    public static String sendXmlPostRequest(String url, String xml, String contentType, String soapAction) {
        HttpURLConnection httpConn;
        BufferedReader reader = null;
        OutputStream out = null;
        String returnXml = "";

        try {
            httpConn = (HttpURLConnection) new URL(url).openConnection();
            httpConn.setRequestProperty("Content-Type", contentType);
            if (null != soapAction) {
                httpConn.setRequestProperty("SOAPAction", soapAction);
            }
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.connect();

            // 获取输出流对象
            out = httpConn.getOutputStream();
            // 将要提交服务器的SOAP请求字符流写入输出流
            httpConn.getOutputStream().write(xml.getBytes("UTF-8"));
            out.flush();

            // 用来获取服务器响应状态
            int code = httpConn.getResponseCode();
            String tempString;
            StringBuffer sb1 = new StringBuffer();
            if (code == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8"));
                while ((tempString = reader.readLine()) != null) {
                    sb1.append(tempString);
                }

            } else {
                reader = new BufferedReader(new InputStreamReader(httpConn.getErrorStream(), "UTF-8"));
                // 一次读入一行，直到读入null为文件结束
                while ((tempString = reader.readLine()) != null) {
                    sb1.append(tempString);
                }

            }

            // 响应报文
            returnXml = sb1.toString();
        } catch (Exception e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    log.error("关闭httpClient出现error：" + e.getMessage(), e);
                }
            }

            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.error("关闭httpClient出现error：" + e.getMessage(), e);
                }
            }
        }
        return returnXml;
    }

    /**
     * 文件上传
     *
     * @param files     上传的文件
     * @param urlString 链接
     * @param parameter 其他参数
     * @return
     */
    public static JSONObject uploadFileToHttpPost(MultipartFile[] files, String urlString, Map<String, String> parameter) {
        JSONObject resultMap = new JSONObject();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            //把一个普通参数和文件上传给下面这个api接口
            HttpPost httpPost = new HttpPost(urlString);
            //设置传输参数,设置编码。设置浏览器兼容模式，解决文件名乱码问题
            MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.RFC6532)
                    .setCharset(StandardCharsets.UTF_8);

            if (files.length != 0) {
                for (int i = 0; i < files.length; i++) {
                    MultipartFile postFile = files[i];
                    //相当于<input type="file" name="media"/>
                    multipartEntity.addBinaryBody(
                            "file" + i,
                            postFile.getInputStream(),
                            ContentType.MULTIPART_FORM_DATA,
                            postFile.getOriginalFilename()
                    );
                }
            }

            //把文件转换成流对象FileBody
            if (!CollectionUtils.isEmpty(parameter)) {
                Set<String> keySet = parameter.keySet();
                for (String key : keySet) {
                    //解决中文乱码
                    ContentType contentType = ContentType.create("text/plain", StandardCharsets.UTF_8);
                    StringBody stringBody = new StringBody(parameter.get(key), contentType);
                    multipartEntity.addPart(key, stringBody);
                }
            }

            HttpEntity reqEntity = multipartEntity.build();
            httpPost.setEntity(reqEntity);

            //发起请求 并返回请求的响应
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                //获取响应对象
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    //打印响应内容
                    String result = EntityUtils.toString(resEntity, StandardCharsets.UTF_8);
                    resultMap = JSONObject.parseObject(result);
                }
                //销毁
                EntityUtils.consume(resEntity);
            } catch (Exception e) {
                log.error("http请求出现error：" + e.getMessage(), e);
            }

        } catch (IOException e) {
            log.error("http请求出现error：" + e.getMessage(), e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("关闭httpClient出现error：" + e.getMessage(), e);
            }
        }
        return resultMap;
    }

    /**
     * 文件上传
     *
     * @param files     上传的文件
     * @param urlString 链接
     * @param parameter 其他参数
     * @return
     */
    public static JSONObject uploadFileToHttpPost(File[] files, String urlString, Map<String, String> parameter) {
        JSONObject resultMap = new JSONObject();
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            //把一个普通参数和文件上传给下面这个api接口
            HttpPost httpPost = new HttpPost(urlString);
            //设置传输参数,设置编码。设置浏览器兼容模式，解决文件名乱码问题
            MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
            if (files.length != 0) {
                for (int i = 0; i < files.length; i++) {
                    File postFile = files[i];
                    FileBody fundFileBin = new FileBody(postFile, ContentType.MULTIPART_FORM_DATA);
                    //相当于<input type="file" name="media"/>
                    multipartEntity.addPart("file" + i, fundFileBin);
                }
            }

            //把文件转换成流对象FileBody
            if (!CollectionUtils.isEmpty(parameter)) {
                Set<String> keySet = parameter.keySet();
                for (String key : keySet) {
                    //解决中文乱码
                    ContentType contentType = ContentType.create("text/plain", StandardCharsets.UTF_8);
                    StringBody stringBody = new StringBody((String) parameter.get(key), contentType);
                    multipartEntity.addPart(key, stringBody);
                }
            }

            HttpEntity reqEntity = multipartEntity.build();
            httpPost.setEntity(reqEntity);
            //发起请求 并返回请求的响应
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                //获取响应对象
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    //打印响应内容
                    String result = EntityUtils.toString(resEntity, StandardCharsets.UTF_8);
                    resultMap = JSONObject.parseObject(result);
                }
                //销毁
                EntityUtils.consume(resEntity);
            } catch (Exception e) {
                log.error("http请求出现error：" + e.getMessage(), e);
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("关闭httpClient出现error：" + e.getMessage(), e);
            }
        }
        return resultMap;
    }

    /**
     * 设置httpPost
     *
     * @param url       请求地址
     * @param param     参数列表
     * @param carryType 携带方式 json, url
     * @return
     */
    private static HttpPost getHttpPost(String url, String param, String carryType) {
        //设置提交参数类型
        HttpPost httpost;

        if ("json".equals(carryType)) {
            httpost = new HttpPost(url);
            httpost.setHeader("Content-Type", "application/json; charset=UTF-8");
            if (StringUtil.isNotBlank(param)) {
                httpost.setEntity(new StringEntity(param, "UTF-8"));
            }
            return httpost;
        }

        if ("url".equals(carryType)) {
            if (StringUtil.isNotBlank(param)) {
                Map json = (Map) JSONObject.parse(param);
                String requestParam = fromMapToUrlParam(json);
                url = url + "?" + requestParam;
            }
            return new HttpPost(url);
        }

        return null;
    }

    /**
     * 根据map生成url， 比如：{"a":"1","b","2"}转换成 b=2&a=1
     *
     * @param map
     * @return
     */
    public static String fromMapToUrlParam(Map<String, String> map) {
        StringBuilder buffer = new StringBuilder();
        if (map.size() > 0) {
            for (String key : map.keySet()) {
                buffer.append(key).append("=");
                if (StringUtil.isBlank(map.get(key))) {
                    buffer.append("&");
                } else {
                    String value = map.get(key);
                    try {
                        value = URLEncoder.encode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    buffer.append(value).append("&");
                }
            }
        }
        // 此处做 substring是因为 转换完成之后，sdString为 b=2&a=1&，所以要去掉最后一个&
        String sdString = buffer.toString();
        return sdString.substring(0, sdString.length() - 1);
    }

    private static HttpClient wrapClient(HttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs,
                                               String string) {
                }


                public void checkServerTrusted(X509Certificate[] xcs,
                                               String string) {
                }


                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            X509HostnameVerifier hv = new X509HostnameVerifier() {


                @Override
                public boolean verify(String hostname, SSLSession session) {
// TODO Auto-generated method stub
                    return true;
                }


                @Override
                public void verify(String arg0, SSLSocket arg1) throws IOException {
// TODO Auto-generated method stub

                }


                @Override
                public void verify(String arg0, X509Certificate arg1)
                        throws SSLException {
// TODO Auto-generated method stub

                }


                @Override
                public void verify(String arg0, String[] arg1, String[] arg2)
                        throws SSLException {
// TODO Auto-generated method stub

                }

            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, (X509HostnameVerifier) hv);

            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 443, ssf));
            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
