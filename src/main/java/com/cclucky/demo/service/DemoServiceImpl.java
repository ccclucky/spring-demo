package com.cclucky.demo.service;

import com.cclucky.mcvframework.annotation.Service;

@Service
public class DemoServiceImpl implements  IDemoService {
    @Override
    public String get(String name) {
        return name;
    }
}
