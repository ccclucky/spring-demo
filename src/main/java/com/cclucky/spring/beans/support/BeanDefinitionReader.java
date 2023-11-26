package com.cclucky.spring.beans.support;

import com.cclucky.spring.beans.config.BeanDefinition;
import com.cclucky.spring.framework.annotation.Controller;
import com.cclucky.spring.framework.annotation.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class BeanDefinitionReader {

    private final Properties contextConfig = new Properties();

    private final List<String> registryBeanClasses = new ArrayList<>();

    public BeanDefinitionReader(String[] configLocations) {
        // 加载配置文件
        doLoadConfig(configLocations[0]);

        // 解析配置文件，将配置信息封装成BeanDefinition
        doScanner(contextConfig.getProperty("scanPackage"));
    }

    // 将配置信息封装成BeanDefinition
    public List<BeanDefinition> doLoadBeanDefinitions() {
        List<BeanDefinition> result = new ArrayList<>();
        try {
            for (String className : registryBeanClasses) {

                Class<?> beanClass = Class.forName(className);

                if (beanClass.isInterface()) continue;

                if (!(beanClass.isAnnotationPresent(Service.class) || beanClass.isAnnotationPresent(Controller.class))) {
                    continue;
                }

                // 默认用类名首字母小写作为beanName
                result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()), beanClass.getName()));

                // 接口的全民给作为beanName
                for (Class<?> i : beanClass.getInterfaces()) {
                    result.add(doCreateBeanDefinition(i.getName(), beanClass.getName()));
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private BeanDefinition doCreateBeanDefinition(String factoryBeanName, String beanClassName) {
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setFactoryBeanName(factoryBeanName);
        beanDefinition.setBeanClassName(beanClassName);
        return beanDefinition;
    }

    private void doLoadConfig(String contextConfigLocation) {
        // 将配置文件转换为文件流读取
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:", ""));

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
                registryBeanClasses.add(className);
            }
        }
    }

    public Properties getConfig() {
        return this.contextConfig;
    }
}
