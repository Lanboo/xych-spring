package com.xych.simple.spring.mvc.framework.v2.servlet;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    private static final String SCAN_PACKAGE = "scanPackage";
    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

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
        
    }

    @Override
    public void init() throws ServletException {
        //1、加载配置文件
        doLoadConfig(getServletConfig().getInitParameter(CONTEXT_CONFIG_LOCATION));
        //2、扫描相关的类
        doScanner(contextConfig.getProperty(SCAN_PACKAGE));
        //3、初始化所有相关的类的实例，并且放入到IOC容器之中
        doInstance();
        //4、完成依赖注入
        doAutowired();
        //5、初始化HandlerMapping
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if(ioc.isEmpty()) {
            return;
        }
        this.ioc.forEach((beanName, obj) -> {
            Class<?> clazz = obj.getClass();
            if(!clazz.isAnnotationPresent(Controller.class)) {
                return;
            }
            String baseUrl = "";
            if(clazz.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = clazz.getAnnotation(RequestMapping.class).value();
            }
            Method[] methods = clazz.getMethods();
            for(Method method : methods) {
                if(method.isAnnotationPresent(RequestMapping.class)) {
                    String url = (baseUrl + "/" + method.getAnnotation(RequestMapping.class).value()).replaceAll("/+", "/");
                    this.handlerMapping.put(url, method);
                    log.info("mapped {},{}", url, method);
                }
            }
        });
    }

    private void doAutowired() {
        try {
            for(Object obj : ioc.values()) {
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
                        field.set(obj, ioc.get(beanName));
                        log.info("set {}.{} by {}", clazz.getName(), field.getName(), beanName);
                    }
                }
            }
        }
        catch(Exception e) {
            log.error("error", e);
        }
    }

    private void doInstance() {
        if(classNames.isEmpty()) {
            return;
        }
        try {
            for(String className : classNames) {
                Class<?> clazz = Class.forName(className);
                log.info("load class {}", className);
                if(clazz.isAnnotationPresent(Controller.class)) {
                    String beanName = clazz.getAnnotation(Controller.class).value();
                    beanName = getBeanName(beanName, clazz);
                    this.ioc.put(beanName, clazz.newInstance());
                    log.info("put class {}", className);
                }
                else if(clazz.isAnnotationPresent(Component.class)) {
                    String beanName = clazz.getAnnotation(Component.class).value();
                    beanName = getBeanName(beanName, clazz);
                    this.ioc.put(beanName, clazz.newInstance());
                    log.info("put class {}", className);
                }
                else if(clazz.isAnnotationPresent(Service.class)) {
                    String beanName = clazz.getAnnotation(Service.class).value();
                    beanName = getBeanName(beanName, clazz);
                    this.ioc.put(beanName, clazz.newInstance());
                    log.info("put {} class {}", beanName, className);
                }
            }
        }
        catch(Exception e) {
            log.error("error", e);
        }
    }

    private String getBeanName(String annotationValue, Class<?> clazz) {
        if(annotationValue == null || annotationValue.length() == 0) {
            // 类名首字母小写
            String simpleName = clazz.getSimpleName();
            char[] chars = simpleName.toCharArray();
            chars[0] += 32;
            return String.valueOf(chars);
        }
        return annotationValue;
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

    private void doLoadConfig(String location) {
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            contextConfig.load(is);
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
}
