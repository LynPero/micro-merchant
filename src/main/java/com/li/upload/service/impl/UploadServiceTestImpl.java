package com.li.upload.service.impl;

import com.li.utils.SSLContextUtils;
import com.li.utils.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class UploadServiceTestImpl {
    public String uploadFile() {
        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/secapi/mch/uploadmedia");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType());
        CloseableHttpClient client = null;
        HttpClient downLoadClient = HttpClients.custom().build();

        String error = null;
        File file = null;
        try {
            HttpGet httpGet = new HttpGet("https://gss0.bdstatic.com/94o3dSag_xI4khGkpoWK1HF6hhy/baike/c0%3Dbaike150%2C5%2C5%2C150%2C50/sign=3137f6103c4e251ff6faecaac6efa272/38dbb6fd5266d0167927ca029b2bd40735fa35d9.jpg");
            //4,执行请求, 获取响应信息
            HttpResponse response = downLoadClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                //得到实体
                HttpEntity entity = response.getEntity();

                byte[] data = EntityUtils.toByteArray(entity);

                //图片存入磁盘
                String fileName = UUID.randomUUID().toString() + ".jpg";
                file = new File(fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                System.out.println("图片下载成功!!!!");
            }
            if (!file.exists()) {
                return null;
            }
            FileInputStream is=new FileInputStream(file);
            client = HttpClients.custom().setSSLContext(SSLContextUtils.getSSLContext("D:\\cer\\wx\\1512272131_20191031_cert\\apiclient_cert.p12", "1512272131")).build();
            // 生成签名和图片md5加密

            String hash = DigestUtils.md5Hex(is);
            Map<String, String> param = new HashMap<>(3);
            param.put("media_hash", hash);
            param.put("mch_id", "1512272131");
            httpPost.addHeader("Content-Type", "multipart/form-data");
            FileBody bin = new FileBody(file, ContentType.create("image/jpg", Consts.UTF_8));
            // 配置post图片上传
            HttpEntity build = MultipartEntityBuilder.create().setCharset(Charset.forName("utf-8"))
                    .addTextBody("media_hash", hash)
                    .addTextBody("mch_id", "1512272131")
                    .addTextBody("sign_type", "HMAC-SHA256")
                    .addTextBody("sign", SignUtil.wechatCertficatesSignBySHA256(param, "sehjwekjasd089ufg029u3j0ijweopir"))
                    .addPart("media", bin)
                    .build();
            httpPost.setEntity(build);
            HttpResponse httpResponse = client.execute(httpPost);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                String responseEntity = EntityUtils.toString(httpResponse.getEntity());
//                log.info("upload response {}", responseEntity);
                System.err.println(responseEntity);
                Document document = DocumentHelper.parseText(responseEntity);
                if ("SUCCESS".equalsIgnoreCase(document.selectSingleNode("//return_code").getStringValue())) {
                    if ("SUCCESS".equalsIgnoreCase(document.selectSingleNode("//result_code").getStringValue())) {
                        return document.selectSingleNode("//media_id").getStringValue();
                    }
                }
//                log.error("上传图片失败，异常信息 code ={} des = {}", document.selectSingleNode("//err_code").getStringValue(), document.selectSingleNode("//err_code_de").getStringValue());
                error = document.selectSingleNode("//err_code_de").getStringValue();
            }
        } catch (Exception e) {
//            log.error("微信图片上传异常 ， e={}", e);
            e.printStackTrace();
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
//                    log.warn("关闭资源httpclient失败 {}", e);
                }
            }
            if (file != null) {
                deleteFile(file);
            }
        }
        return error;
    }

    /**
     * 删除临时文件
     *
     * @param files
     */
    private void deleteFile(File... files) {
        for (File file : files) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static void main(String[] args) {
        System.err.println(new UploadServiceTestImpl().uploadFile());
    }
}
