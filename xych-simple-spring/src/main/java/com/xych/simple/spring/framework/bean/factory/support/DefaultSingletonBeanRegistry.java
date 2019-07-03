package com.xych.simple.spring.framework.bean.factory.support;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.xych.simple.spring.framework.bean.factory.config.SingletonBeanRegistry;

public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

    @Override
    public void registerSingleton(String beanName, Object singletonObject) {
        synchronized(this.singletonObjects) {
            Object oldObject = this.singletonObjects.get(beanName);
            if(oldObject != null) {
                throw new RuntimeException("Could not register object [" + singletonObject + "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
            }
            addSingleton(beanName, singletonObject);
        }
    }

    protected void addSingleton(String beanName, Object singletonObject) {
        synchronized(this.singletonObjects) {
            this.singletonObjects.put(beanName, singletonObject);
            this.registeredSingletons.add(beanName);
        }
    }

    @Override
    public Object getSingleton(String beanName) {
        Object singletonObject = this.singletonObjects.get(beanName);
        boolean falg = true;// 如果为空，且正在创建
        if(singletonObject == null && falg) {
            // TODO 通过ObjectFactory创建，这里暂不处理
        }
        return singletonObject;
    }

    @Override
    public boolean containsSingleton(String beanName) {
        return this.singletonObjects.containsKey(beanName);
    }

    @Override
    public String[] getSingletonNames() {
        synchronized(this.singletonObjects) {
            return this.registeredSingletons.toArray(new String[0]);
        }
    }

    @Override
    public int getSingletonCount() {
        synchronized(this.singletonObjects) {
            return this.registeredSingletons.size();
        }
    }
}
