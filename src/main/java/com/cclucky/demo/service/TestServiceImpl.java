package com.cclucky.demo.service;

import com.cclucky.mcvframework.annotation.Autowired;
import com.cclucky.mcvframework.annotation.Service;

@Service
public class TestServiceImpl implements  ITestService{

    @Autowired
    private  IDemoService demoService;
    @Override
    public String query(String name) {
        String str= demoService.get(name);
        System.out.println("----------------------"+str+"---------------------");
        return "==================="+name+"======================";
    }


}
