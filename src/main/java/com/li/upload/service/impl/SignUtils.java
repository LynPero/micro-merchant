package com.li.upload.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <pre>
 * 签名相关工具类.
 * Created by Binary Wang on 2017-3-23.
 * </pre>
 *
 * @author <a href="https://github.com/binarywang">binarywang(Binary Wang)</a>
 */
@Slf4j
public class SignUtils {


    /**
     * 微信支付签名算法(详见:https://pay.weixin.qq.com/wiki/doc/api/tools/cash_coupon.php?chapter=4_3).
     *
     * @param params        参数信息
     * @param signKey       签名Key
     * @return 签名字符串 string
     */
    public static String createSign(Map<String, String> params,  String signKey) {
        SortedMap<String, String> sortedMap = new TreeMap<>(params);

        StringBuilder toSign = new StringBuilder();
        for (String key : sortedMap.keySet()) {
            String value = params.get(key);
            boolean shouldSign = false;
            if (StringUtils.isNotEmpty(value)) {
                shouldSign = true;
            }

            if (shouldSign) {
                toSign.append(key).append("=").append(value).append("&");
            }
        }

        toSign.append("key=").append(signKey);

        return DigestUtils.md5Hex(toSign.toString()).toUpperCase();
    }

}
