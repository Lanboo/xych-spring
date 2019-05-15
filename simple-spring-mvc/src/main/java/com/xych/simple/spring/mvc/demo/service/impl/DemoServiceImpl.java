package com.xych.simple.spring.mvc.demo.service.impl;

import com.xych.simple.spring.mvc.demo.service.DemoService;
import com.xych.simple.spring.mvc.framework.annotation.Service;

@Service("demoService")
public class DemoServiceImpl implements DemoService {
    @Override
    public String getName(String name) {
        return "My name is " + name;
    }
}
