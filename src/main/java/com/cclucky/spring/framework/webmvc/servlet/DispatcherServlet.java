package com.cclucky.spring.framework.webmvc.servlet;

import com.cclucky.spring.framework.annotation.*;
import com.cclucky.spring.framework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DispatcherServlet extends HttpServlet {

    private ApplicationContext context;

    private final List<HandlerMapping> handlerMappings = new ArrayList<>();

    private final List<ViewResolver> viewResolvers = new ArrayList<>();

    private final Map<HandlerMapping, HandlerAdapter> handlerAdapters = new HashMap<>();

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
            Map<String, Object> map = new HashMap<>();
            map.put("detail", "500 Exception");
            map.put("stackTrace", Arrays.toString(e.getStackTrace()));
            processDispatchResult(req, resp, new ModelAndView("500", map));
        }
    }

//    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
//        // 获取用户请求的url
//        String url = req.getRequestURI();
//        // 转换为相对路径
//        String contextPath = req.getContextPath();
//        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
//
//        // 容器中是否有该路径存在
//        if (!this.handleMapping.containsKey(url)) {
//            resp.getWriter().write("404 Not Found");
//            return;
//        }
//
//        // 获取参数
//        Map<String, String[]> params = req.getParameterMap();
//
//        Method method = this.handleMapping.get(url);
//
//        // Method的形参列表
//        Class<?>[] paramTypes = method.getParameterTypes();
//        // Method的实参列表
//        Object[] paramValues = new Object[paramTypes.length];
//
//        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
//
//        // 为形参赋值
//        for (int i = 0; i < paramTypes.length; i++) {
//            Class<?> paramType = paramTypes[i];
//            if (paramType == HttpServletRequest.class) {
//                paramValues[i] = req;
//            } else if (paramType == HttpServletResponse.class) {
//                paramValues[i] = resp;
//            } else if (paramType == String.class) {
//                for (Annotation a : parameterAnnotations[i]) {
//                    if (a instanceof RequestParam) {
//                        String paramName = ((RequestParam) a).value();
//                        String value = Arrays.toString(params.get(paramName))
//                                .replaceAll("[\\[\\]]", "")
//                                .replaceAll("\\s", "");
//                        paramValues[i] = value;
//                    }
//                }
//            } else {
//                paramValues[i] = null;
//            }
//        }
//
//        method.invoke(this.context.getBean(method.getDeclaringClass()), paramValues);
//    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException, IOException {
        // 根据url获取handlerMapping对象
        HandlerMapping mappedHandler = getHandler(req);
        if (mappedHandler == null) {
            // 返回404
            processDispatchResult(req, resp, new ModelAndView("404"));
            return;
        }

        // 根据HandlerMapping, 获取一个handlerAdapter
        HandlerAdapter ha = getHandlerAdapter(mappedHandler);

        // 根据HandlerAdapter的方法动态匹配参数并得到返回值ModelAndView
        ModelAndView mv = Objects.requireNonNull(ha).handle(req, resp, mappedHandler);

        // 根据ModelAndView决定选择哪个ViewResolver进行解析和渲染
        processDispatchResult(req, resp, mv);

    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, ModelAndView mv) throws IOException {
        if (null == mv) return;
        if (this.viewResolvers.isEmpty()) return;

        for (ViewResolver viewResolver : this.viewResolvers) {
            if (!mv.getViewName().equals(viewResolver.getName())) continue;

            View view = viewResolver.resolverViewName(mv.getViewName());
            view.render(mv.getModel(), req, resp);
        }
    }

    private HandlerAdapter getHandlerAdapter(HandlerMapping mappedHandler) {
        if (this.handlerAdapters.isEmpty()) return null;
        return this.handlerAdapters.get(mappedHandler);
    }

    private HandlerMapping getHandler(HttpServletRequest req) {
        // 获取用户请求的url
        String url = req.getRequestURI();
        // 转换为相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        for (HandlerMapping handlerMapping : this.handlerMappings) {
            Matcher matcher = handlerMapping.getPattern().matcher(url);
            if (!matcher.matches()) continue;
            return handlerMapping;
        }
        return null;
    }

    private void initStrategies(ApplicationContext applicationContext) throws ClassNotFoundException {

        //通过HandlerMapping 将请求影射到控制器
        //HandlerMapping用来保存Controller中配置的RequestMapping和Method的一个对应关系
        initHandlerMappings(applicationContext);
        //通过HandlerAdapter 进行多类型参数动态匹配
        //HandlerAdapter 用来动态匹配Method参数，包括类型转换，动态赋值
        initHandlerAdapters(applicationContext);
        //通过ViewResolver解析逻辑视图到具体视图实现
        //通过ViewResolver实现动态模板解析
        initViewResolvers(applicationContext);
    }

    private void initViewResolvers(ApplicationContext applicationContext) {
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource(templateRoot)).getFile();
        File templateRootDir = new File(templateRootPath);

        for (File file : Objects.requireNonNull(templateRootDir.listFiles())) {
            this.viewResolvers.add(new ViewResolver(templateRoot, file.getName()));
        }
    }

    private void initHandlerAdapters(ApplicationContext applicationContext) {
        // 将HandlerMapping 和 HandlerAdapter 建立 1对1 关系
        for (HandlerMapping handlerMapping : handlerMappings) {
            this.handlerAdapters.put(handlerMapping, new HandlerAdapter());
        }
    }

    private void initHandlerMappings(ApplicationContext applicationContext) throws ClassNotFoundException {
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
                String regex = ("/" + baseUrl + "/" + requestMapping.value())
                        .replaceAll("\\*", ".*")
                        .replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);

                handlerMappings.add(new HandlerMapping(pattern, instance, method));

                System.out.println("Mapped:" + pattern + ", " + method);
            }
        }
    }

    @Override
    public void init(ServletConfig config) {
        // IoC、 Di
        context = new ApplicationContext(config.getInitParameter("contextConfigLocation"));

        // MVC
        try {
            initStrategies(context);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("spring framework is init");
    }
}
