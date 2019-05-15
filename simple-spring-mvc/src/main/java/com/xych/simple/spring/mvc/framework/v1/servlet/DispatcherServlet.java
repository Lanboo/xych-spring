package com.xych.simple.spring.mvc.framework.v1.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xych.simple.spring.mvc.framework.annotation.Autowired;
import com.xych.simple.spring.mvc.framework.annotation.Component;
import com.xych.simple.spring.mvc.framework.annotation.Controller;
import com.xych.simple.spring.mvc.framework.annotation.RequestMapping;
import com.xych.simple.spring.mvc.framework.annotation.Service;
import com.xych.simple.spring.mvc.uitls.StrUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DispatcherServlet extends HttpServlet {
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> mapping = new HashMap<>();
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        }
        catch(Exception e) {
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if(!this.mapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        Method method = (Method) this.mapping.get(url);
        Class<?> beanClass = method.getDeclaringClass();
        if(!beanClass.isAnnotationPresent(Controller.class)) {
            resp.getWriter().write("500 method mapping error");
            return;
        }
        String beanName = beanClass.getAnnotation(Controller.class).value();
        beanName = StrUtils.getStr(beanName, beanClass.getName());
        Object obj = this.mapping.get(beanName);
        if(obj == null) {
            resp.getWriter().write("500 not find object");
            return;
        }
        // 此处对参数未处理
        @SuppressWarnings("unchecked")
        Map<String, String[]> params = req.getParameterMap();
        method.invoke(obj, new Object[] { req, resp, params.get("name")[0] });
    }

    @Override
    public void init() throws ServletException {
        String location = getServletConfig().getInitParameter("contextConfigLocation");
        location = StrUtils.getStr(location, "application.properties");
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            Properties contentProperties = new Properties();
            contentProperties.load(is);
            String scanPackage = contentProperties.getProperty("scanPackage");
            doScanner(scanPackage);
            for(String className : classNames) {
                Class<?> clazz = Class.forName(className);
                log.info("load class {}", className);
                if(clazz.isAnnotationPresent(Controller.class)) {
                    String beanName = clazz.getAnnotation(Controller.class).value();
                    beanName = StrUtils.getStr(beanName, className);
                    this.mapping.put(beanName, clazz.newInstance());
                    log.info("put class {}", className);
                    String baseUrl = "";
                    if(clazz.isAnnotationPresent(RequestMapping.class)) {
                        baseUrl = clazz.getAnnotation(RequestMapping.class).value();
                    }
                    Method[] methods = clazz.getMethods();
                    for(Method method : methods) {
                        if(method.isAnnotationPresent(RequestMapping.class)) {
                            String url = (baseUrl + "/" + method.getAnnotation(RequestMapping.class).value()).replaceAll("/+", "/");
                            this.mapping.put(url, method);
                            log.info("mapped {},{}", url, method);
                        }
                    }
                }
                else if(clazz.isAnnotationPresent(Component.class)) {
                    String beanName = clazz.getAnnotation(Component.class).value();
                    beanName = StrUtils.getStr(beanName, className);
                    this.mapping.put(beanName, clazz.newInstance());
                }
                else if(clazz.isAnnotationPresent(Service.class)) {
                    String beanName = clazz.getAnnotation(Service.class).value();
                    beanName = StrUtils.getStr(beanName, className);
                    this.mapping.put(beanName, clazz.newInstance());
                }
            }
            for(Object obj : mapping.values()) {
                if(obj == null) {
                    continue;
                }
                Class<?> clazz = obj.getClass();
                if(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Service.class)) {
                    Field[] fields = clazz.getDeclaredFields();
                    for(Field field : fields) {
                        if(!field.isAnnotationPresent(Autowired.class)) {
                            continue;
                        }
                        String beanName = field.getAnnotation(Autowired.class).value();
                        if(beanName == null || "".equals(beanName)) {
                            beanName = field.getType().getName();
                        }
                        field.setAccessible(true);
                        field.set(obj, mapping.get(beanName));
                        log.info("set {}.{} by {}", clazz.getName(), field.getName(), beanName);
                    }
                }
            }
        }
        catch(Exception e) {
            log.error("error", e);
        }
        finally {
            if(is != null) {
                try {
                    is.close();
                }
                catch(IOException e) {
                    log.error("io close error", e);
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for(File file : classDir.listFiles()) {
            if(file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            }
            else if(file.getName().endsWith(".class")) {
                String className = scanPackage + "." + file.getName().replace(".class", "");
                this.classNames.add(className);
                log.info("find class {}", className);
            }
        }
    }
}
