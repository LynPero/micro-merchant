package com.li.upload.service.impl;

import com.li.utils.SSLContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UploadServiceTestImpl {
    public String uploadFile() {
        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/secapi/mch/uploadmedia");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType());
        CloseableHttpClient client = null;
        File excelFile = null;
        String error = null;
        try {
            client = HttpClients.custom().setSSLContext(SSLContextUtils.getSSLContext("D:\\cer\\wx\\1512272131_20191031_cert\\apiclient_cert.p12", "1512272131")).build();
            // 生成签名和图片md5加密
            UrlResource urlResource = new UrlResource("https://gss0.bdstatic.com/94o3dSag_xI4khGkpoWK1HF6hhy/baike/c0%3Dbaike150%2C5%2C5%2C150%2C50/sign=3137f6103c4e251ff6faecaac6efa272/38dbb6fd5266d0167927ca029b2bd40735fa35d9.jpg");
            InputStream is = urlResource.getInputStream();
            String hash = DigestUtils.md5Hex(is);
            Map<String, String> param = new HashMap<>(3);
            param.put("media_hash", hash);
            param.put("mch_id", "1512272131");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            // 配置post图片上传
            HttpEntity build = MultipartEntityBuilder.create().setCharset(Charset.forName("utf-8"))
                    .addTextBody("media_hash", hash)
                    .addTextBody("mch_id", "1512272131")
                    .addTextBody("sign", SignUtils.createSign(param, "sehjwekjasd089ufg029u3j0ijweopir"))
                    .addBinaryBody("media", is, ContentType.MULTIPART_FORM_DATA, urlResource.getFilename())
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
            if (excelFile != null) {
                deleteFile(excelFile);
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
