package com.cclucky.demo.action;

import com.cclucky.demo.service.ITestService;
import com.cclucky.spring.framework.annotation.Autowired;
import com.cclucky.spring.framework.annotation.Controller;
import com.cclucky.spring.framework.annotation.RequestMapping;
import com.cclucky.spring.framework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/test")
public class TestAction {

    @Autowired
    private ITestService testService;

    @RequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse res, @RequestParam("name") String name, String address) throws IOException {
        String result = testService.query(name);
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("address", address);
        model.put("data", result);
        res.getWriter().write(String.valueOf(model));
    }


}
