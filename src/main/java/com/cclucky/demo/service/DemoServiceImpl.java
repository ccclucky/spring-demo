package com.cclucky.demo.service;

import com.cclucky.spring.framework.annotation.Service;

@Service
public class DemoServiceImpl implements  IDemoService {
    @Override
    public String get(String name) {
        return name;
    }
}
