package com.cclucky.mcvframework.servlet;

import com.cclucky.mcvframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    private final Map<String, Object> ioc = new HashMap<>();

    private final Properties contextConfig = new Properties();

    private final List<String> classNames = new ArrayList<>();

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

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    @Override
    public void init(ServletConfig config) {
        // 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        // Bean实例化，并将实例对象缓存到Ioc容器中
        doInstance();

        // 完成依赖注入
        doAutowired();

        // 初始化HandleMapping, 匹配路径和方法
        doInitHandleMapping();

        System.out.println("spring framework is init");
    }

    private void doInitHandleMapping() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

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

    private void doAutowired() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // Declare可以把所有的包括public、default、private、protect的属性都获取出来
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) continue;

                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if (beanName.isEmpty()) {
                    beanName = field.getType().getName();
                }

                // 只有public修饰的类才可以外部赋值，进行赋权操作
                field.setAccessible(true);

                try {
                    // 根据beanName从ioc容器获取实例对象并赋予
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) return;

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                // 只有生命要生成bean才是实例
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object instance = clazz.newInstance();
                    // 默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    // 1、默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    // 2、不同包下的相同类名
                    Service service = clazz.getAnnotation(Service.class);
                    if (!("".equals(service.value()))) {
                        beanName = service.value();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    // 3、接口则 new 实现类
                    for (Class<?> aClass : clazz.getInterfaces()) {
                        if (ioc.containsKey(aClass.getName())) {
                            throw new RuntimeException("The beanName is exist");
                        }
                        ioc.put(aClass.getName(), instance);
                    }
                } else {
                    continue;
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL url = classLoader.getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(Objects.requireNonNull(url).getFile());

        for (File file : Objects.requireNonNull(classPath.listFiles())) {
            // 如果是子目录就递归
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) continue;
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        // 将配置文件转换为文件流读取
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!Objects.isNull(resourceAsStream)) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
