package com.cclucky.spring.framework.webmvc.servlet;

import com.cclucky.spring.framework.annotation.*;
import com.cclucky.spring.framework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class DispatcherServlet extends HttpServlet {

    ApplicationContext context;

    private final Map<String, Method> handleMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 调用
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 真正调用，负责请求分发，匹配url和方法
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception, Detail:" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        // 获取用户请求的url
        String url = req.getRequestURI();
        // 转换为相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        // 容器中是否有该路径存在
        if (!this.handleMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found");
            return;
        }

        // 获取参数
        Map<String, String[]> params = req.getParameterMap();

        Method method = this.handleMapping.get(url);

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

        method.invoke(this.context.getBean(method.getDeclaringClass()), paramValues);
    }

    @Override
    public void init(ServletConfig config) {
        context = new ApplicationContext(config.getInitParameter("contextConfigLocation"));

        // 初始化HandleMapping, 匹配路径和方法
        doInitHandleMapping();

        System.out.println("spring framework is init");
    }

    private void doInitHandleMapping() {
        if (this.context.getBeanDefinitionCount() == 0) return;

        String[] beanNames = this.context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object instance = this.context.getBean(beanName);
            Class<?> clazz = instance.getClass();

            if (!clazz.isAnnotationPresent(Controller.class)) continue;

            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = clazz.getAnnotation(RequestMapping.class).value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)) continue;

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handleMapping.put(url, method);

                System.out.println("Mapped:" + url + ", " + method);
            }
        }
    }
}
