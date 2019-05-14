package com.xych.simple.spring.mvc.uitls;

public class StrUtils {
    /**
     * 如果空，获取默认值
     * @Author WeiXiaowei
     * @CreateDate 2018年10月15日下午7:05:07
     */
    public static String getStr(String value, String defaultValue) {
        if(value == null || value.length() == 0) {
            return value;
        }
        return defaultValue;
    }
}
