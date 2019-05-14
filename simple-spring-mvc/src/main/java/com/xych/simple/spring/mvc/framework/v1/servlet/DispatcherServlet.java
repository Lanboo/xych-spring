package com.xych.simple.spring.mvc.framework.v1.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        }
        catch(Exception e) {
            log.error("error", e);
        }
        finally {
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
