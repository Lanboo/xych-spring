package com.xych.simple.spring.mvc.demo.controller.v3;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xych.simple.spring.mvc.demo.service.DemoService;
import com.xych.simple.spring.mvc.framework.annotation.Autowired;
import com.xych.simple.spring.mvc.framework.annotation.Controller;
import com.xych.simple.spring.mvc.framework.annotation.RequestMapping;
import com.xych.simple.spring.mvc.framework.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/v3/demo")
@Slf4j
public class DemoController {
    @Autowired("demoService")
    DemoService demoService;

    @RequestMapping("/name")
    public void getName(HttpServletRequest req, HttpServletResponse resp, @RequestParam("name") String name) {
        String result = demoService.getName(name);
        try {
            resp.getWriter().write(result);
        }
        catch(IOException e) {
            log.error("", e);
        }
    }
}
