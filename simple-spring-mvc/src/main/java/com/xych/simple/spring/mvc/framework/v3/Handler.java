package com.xych.simple.spring.mvc.framework.v3;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xych.simple.spring.mvc.framework.annotation.RequestParam;

import lombok.Data;

@Data
public class Handler {
    private Pattern pattern;
    private Method method;
    private Object controller;
    private Map<String, Integer> paramIndexMapping;// 参数顺序

    public Handler(Pattern pattern, Method method, Object controller) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        this.paramIndexMapping = new HashMap<>();
        putIndexMapping(method);
    }

    public Object invoke(Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return this.method.invoke(this.controller, args);
    }

    private void putIndexMapping(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        for(int i = 0, len = paramTypes.length; i < len; i++) {
            Class<?> paramType = paramTypes[i];
            String paramName = "";
            if(HttpServletRequest.class.isAssignableFrom(paramType) || HttpServletResponse.class.isAssignableFrom(paramType)) {
                paramName = paramType.getName();
            }
            else if(paramType == String.class) {
                Annotation[] annotations = paramsAnnotations[i];
                for(Annotation annotation : annotations) {
                    if(annotation instanceof RequestParam) {
                        paramName = ((RequestParam) annotation).value();
                        break;
                    }
                }
            }
            else {
                // 其他类型参数不处理
                throw new RuntimeException("param tpye is not supported");
            }
            if(paramName == null || paramName.length() == 0) {
                throw new RuntimeException("param is not @RequestParam");
            }
            this.paramIndexMapping.put(paramName, i);
        }
    }
}
