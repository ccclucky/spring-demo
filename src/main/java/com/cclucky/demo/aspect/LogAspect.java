package com.cclucky.demo.aspect;

public class LogAspect {
    public void before() {
        System.out.println("========before=======");
    }
    public void after() {
        System.out.println("========after=======");
    }
    public void exception() {
        System.out.println("========after exception=======");
    }
}
