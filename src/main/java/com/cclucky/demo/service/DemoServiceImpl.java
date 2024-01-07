package com.cclucky.demo.service;

import com.cclucky.spring.framework.annotation.Autowired;
import com.cclucky.spring.framework.annotation.Service;

@Service
public class DemoServiceImpl implements  IDemoService {

    @Autowired
    private ITestService testService;

    @Override
    public String get(String name) {
        return name;
    }
}
