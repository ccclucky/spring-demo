package com.cclucky.spring.framework.context;

import com.cclucky.spring.beans.BeanWrapper;
import com.cclucky.spring.beans.config.BeanDefinition;
import com.cclucky.spring.beans.support.BeanDefinitionReader;
import com.cclucky.spring.framework.annotation.Autowired;
import com.cclucky.spring.framework.annotation.Controller;
import com.cclucky.spring.framework.annotation.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {

    private final String[] configLocations;

    private BeanDefinitionReader reader;

    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private final Map<String, BeanWrapper> factoryBeanInstanceCache = new HashMap<>();
    private final Map<String, Object> factoryBeanObjectCache = new HashMap<>();

    public ApplicationContext(String... configLocations) {
        this.configLocations = configLocations;

        try {
            // 加载配置文件
            this.reader = new BeanDefinitionReader(this.configLocations);
            List<BeanDefinition> beanDefinitions = this.reader.doLoadBeanDefinitions();

            // 将BeanDefinition对象放入缓存
            doRegistryBeanDefinition(beanDefinitions);

            // 创建IoC容器
            doCreateBean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doCreateBean() {
        for (Map.Entry<String, BeanDefinition> beanDefinition : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinition.getKey();
            getBean(beanName);
        }
    }

    private void doRegistryBeanDefinition(List<BeanDefinition> beanDefinitions) throws Exception {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception("The " + beanDefinition.getFactoryBeanName() + " is exist");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
        }
    }

    // 创建bean实例和依赖注入
    public Object getBean(String beanName) {
        // 取出beanName对应的配置信息
        BeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        // 根据配置信息创建bean实例
        Object instance = instantiateBean(beanName, beanDefinition);
        // 如果实例为空
        if (instance == null) return null;
        // 不为空则封装成BeanWrapper
        BeanWrapper beanWrapper = new BeanWrapper(instance);
        // 将BeanWrapper对象缓存到IoC容器中
        this.factoryBeanInstanceCache.put(beanName, beanWrapper);
        // 完成依赖注入
        populateBean(beanName, beanDefinition, beanWrapper);
        return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
    }

    private void populateBean(String beanName, BeanDefinition beanDefinition, BeanWrapper beanWrapper) {

        Object instance = beanWrapper.getWrapperInstance();
        Class<?> clazz = beanWrapper.getWrapperClass();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;

            Autowired autowired = field.getAnnotation(Autowired.class);
            String AutowiredBeanName = autowired.value().trim();
            if (AutowiredBeanName.isEmpty()) {
                AutowiredBeanName = field.getType().getName();
            }

            // 只有public修饰的类才可以外部赋值，进行赋权操作
            field.setAccessible(true);

            try {
                if (!this.factoryBeanInstanceCache.containsKey(AutowiredBeanName)) continue;
                // 根据beanName从ioc容器获取实例对象并赋予
                field.set(instance, this.factoryBeanInstanceCache.get(AutowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Object instantiateBean(String beanName, BeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object instance = null;

        try {
            Class<?> clazz = Class.forName(className);
            if (!(clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Controller.class))) {
                return null;
            }

            // 创建原生对象
            instance = clazz.newInstance();

            // AOP切面表达式匹配

            // 三级缓存
            this.factoryBeanObjectCache.put(beanName, instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    public Object getBean(Class className) {
        return getBean(className.getName());
    }

    /**
     * 判断IoC容器是否为空
     * @return int
     */
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[0]);
    }
}
