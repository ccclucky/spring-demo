package com.cclucky.spring.framework.webmvc.servlet;

import com.cclucky.spring.framework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class HandlerAdapter {
    // 完成动态参数的匹配
    public ModelAndView handle(HttpServletRequest req, HttpServletResponse resp, HandlerMapping mappedHandler)
            throws InvocationTargetException, IllegalAccessException {
        // 获取参数
        Map<String, String[]> params = req.getParameterMap();

        Method method = mappedHandler.getMethod();

        // Method的形参列表
        Class<?>[] paramTypes = method.getParameterTypes();
        // Method的实参列表
        Object[] paramValues = new Object[paramTypes.length];

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // 为形参赋值
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (paramType == HttpServletRequest.class) {
                paramValues[i] = req;
            } else if (paramType == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else if (paramType == String.class) {
                for (Annotation a : parameterAnnotations[i]) {
                    if (a instanceof RequestParam) {
                        String paramName = ((RequestParam) a).value();
                        String value = Arrays.toString(params.get(paramName))
                                .replaceAll("[\\[\\]]", "")
                                .replaceAll("\\s", "");
                        paramValues[i] = value;
                    }
                }
            } else {
                paramValues[i] = null;
            }
        }

        Object result = method.invoke(mappedHandler.getController(), paramValues);
        if (result == null || result instanceof Void) return null;
        boolean isModelAndView = mappedHandler.getMethod().getReturnType() == ModelAndView.class;
        if (isModelAndView) {
            return (ModelAndView) result;
        }
        return null;
    }
}
