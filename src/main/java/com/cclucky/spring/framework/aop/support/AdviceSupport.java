package com.cclucky.spring.framework.aop.support;

import com.cclucky.spring.framework.aop.aspect.Advice;
import com.cclucky.spring.framework.aop.config.AopConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdviceSupport {

    private Class<?> targetClass;
    private Object target;
    private AopConfig config;
    private Pattern pointCutClassPattern;
    private Map<Method, Map<String, Advice>> methodCache;


    public AopConfig getConfig() {
        return config;
    }

    public void setConfig(AopConfig config) {
        this.config = config;
    }

    public AdviceSupport(AopConfig config) {
        this.config = config;
    }

    private void parse() {

        // 确认目标类名是否满足切面规则
        String pointCutRegex = this.config.getPointCut()
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\\\.\\*", ".*")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)");

        String pointCutForClassRegex = pointCutRegex.substring(0, pointCutRegex.lastIndexOf("\\(") - 4)
                .replaceAll("\\\\", "");

        pointCutClassPattern = Pattern.compile(pointCutForClassRegex.substring(pointCutForClassRegex.lastIndexOf(" ") + 1));

        // 建立切面和业务代码的关联关系
        methodCache = new HashMap<>();

        // 将切面逻辑通过反射机制读取出来先缓存
        try {
            Map<String, Method> aspectMethods = new HashMap<>();
            Class<?> aspectClass = Class.forName(this.config.getAspectClass());

            for (Method method : aspectClass.getMethods()) {
                aspectMethods.put(method.getName(), method);
            }

            // 使用正则匹配目标类的功能方法，满足切面表达式
            Pattern pointCutPattern = Pattern.compile(pointCutRegex.replaceAll("\\\\", ""));
            for (Method method : this.targetClass.getMethods()) {
                String methodString = method.toString();
                if (methodString.contains("throw")) {
                    methodString = methodString.substring(0, methodString.lastIndexOf("throw")).trim();
                }
                Matcher matcher = pointCutPattern.matcher(methodString);
                if (matcher.matches()) {
                    Map<String, Advice> advices = new HashMap<>();
                    if (!(null == this.config.getAspectBefore() || "".equals(this.config.getAspectBefore()))) {
                        advices.put("before",
                                new Advice(aspectClass.newInstance(), aspectMethods.get(this.config.getAspectBefore())));
                    }
                    if (!(null == this.config.getAspectAfter() || "".equals(this.config.getAspectAfter()))) {
                        advices.put("after",
                                new Advice(aspectClass.newInstance(), aspectMethods.get(this.config.getAspectAfter())));
                    }
                    if (!(null == this.config.getAspectAfterThrow() || "".equals(this.config.getAspectAfterThrow()))) {
                        Advice advice = new Advice(aspectClass.newInstance(), aspectMethods.get(this.config.getAspectAfterThrow()));
                        advice.setThrowName(this.config.getAspectAfterThrowingName());
                        advices.put("afterThrow", advice);
                    }
                    methodCache.put(method, advices);
                }
            }

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

    }

    public boolean pointCutMatch() {
        return this.pointCutClassPattern.matcher(this.targetClass.getName()).matches();
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Object getTarget() {
        return target;
    }

    public void setTargetClass(Class<?> clazz) {
        this.targetClass = clazz;
        parse();
    }

    public void setTarget(Object instance) {
        this.target = instance;
    }

    public Map<String, Advice> getAdvices(Method method, Class<?> targetClass) throws NoSuchMethodException {
        Map<String, Advice> cache = this.methodCache.get(method);
        if (null == cache) {
            Method m = targetClass.getMethod(method.getName(), method.getParameterTypes());
            cache = methodCache.get(m);
            this.methodCache.put(m, cache);
        }
        return cache;
    }
}
