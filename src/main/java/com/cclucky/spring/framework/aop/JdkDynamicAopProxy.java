package com.cclucky.spring.framework.aop;

import com.cclucky.spring.framework.aop.aspect.Advice;
import com.cclucky.spring.framework.aop.support.AdviceSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class JdkDynamicAopProxy implements InvocationHandler {

    private AdviceSupport config;

    public JdkDynamicAopProxy(AdviceSupport config) {
        this.config = config;
    }

    public Object getProxy() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Class<?>[] interfaces = this.config.getTargetClass().getInterfaces();
        return Proxy.newProxyInstance(classLoader, interfaces, this);
    }

    @Override
    public String toString() {
        return "JdkDynamicAopProxy{" +
                "config=" + config +
                '}';
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Map<String, Advice> advices = this.config.getAdvices(method, this.config.getTargetClass());

        // 非功能代码
        invokeAdvice(advices.get("before"));

        // 功能性代码
        Object returnValue = null;
        try {
            returnValue = method.invoke(this.config.getTarget(), args);
        } catch (Exception e) {
            e.printStackTrace();
            // 非功能代码
            invokeAdvice(advices.get("afterThrow"));
        }

        // 非功能代码
        invokeAdvice(advices.get("after"));

        return returnValue;
    }

    private void invokeAdvice(Advice advice) {
        try {
            advice.getAdviceMethod().invoke(advice.getAspect());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
