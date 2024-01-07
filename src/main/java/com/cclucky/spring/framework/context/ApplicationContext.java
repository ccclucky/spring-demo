package com.cclucky.spring.framework.context;

import com.cclucky.spring.beans.BeanWrapper;
import com.cclucky.spring.beans.config.BeanDefinition;
import com.cclucky.spring.beans.support.BeanDefinitionReader;
import com.cclucky.spring.framework.annotation.Autowired;
import com.cclucky.spring.framework.aop.JdkDynamicAopProxy;
import com.cclucky.spring.framework.aop.config.AopConfig;
import com.cclucky.spring.framework.aop.support.AdviceSupport;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ApplicationContext {

    private final String[] configLocations;

    private BeanDefinitionReader reader;

    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private final Map<String, BeanWrapper> factoryBeanInstanceCache = new HashMap<>();      // 一级缓存
    private final Map<String, Object> factoryBeanObjectCache = new HashMap<>();           // 二级缓存

    public ApplicationContext(String... configLocations) {
        // ApplicationContext 的初始化
        // 加载配置文件，读取 Bean 定义，创建 IoC 容器
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


    private void doCreateBean() throws ClassNotFoundException {
        // 加载 Bean 定义并创建 IoC 容器
        for (Map.Entry<String, BeanDefinition> beanDefinition : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinition.getKey();
            getBean(beanName);
        }
    }

    private void doRegistryBeanDefinition(List<BeanDefinition> beanDefinitions) throws Exception {
        // 注册 BeanDefinition
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception("The " + beanDefinition.getFactoryBeanName() + " is exist");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
        }
    }

    // 创建bean实例和依赖注入
    public Object getBean(String beanName) throws ClassNotFoundException {
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
//        populateBean(beanName, beanDefinition, beanWrapper);
        return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
    }

    private void populateBean(String beanName, BeanDefinition beanDefinition, BeanWrapper beanWrapper) {
        // 遍历 Bean 类的字段，进行 Autowired 注解的依赖注入
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

            if (AutowiredBeanName.contains("ITestService")) {
                System.out.println("==============");
            }
            if (AutowiredBeanName.contains("IDemoService")) {
                System.out.println("==============");
            }
            try {
                if (this.factoryBeanInstanceCache.containsKey(AutowiredBeanName)) {
                    // 根据beanName从ioc容器获取实例对象并赋予
                    field.set(instance, this.factoryBeanInstanceCache.get(AutowiredBeanName).getWrapperInstance());
                } else if (this.factoryBeanObjectCache.containsKey(AutowiredBeanName)) {
                    field.set(instance, this.factoryBeanObjectCache.get(AutowiredBeanName));
                } else {
                    getBean(AutowiredBeanName);
                    field.set(instance, this.factoryBeanInstanceCache.get(AutowiredBeanName).getWrapperInstance());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 创建 Bean 实例
    private Object instantiateBean(String beanName, BeanDefinition beanDefinition) {
        // 通过反射创建原生对象，完成依赖注入，处理 AOP 相关配置，生成代理对象
        String className = beanDefinition.getBeanClassName();
        Object instance = null;

        if (this.factoryBeanInstanceCache.containsKey(beanName)) {
            return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
        }

        try {
            //三级缓存
            Class<?> clazz = Class.forName(className);

            // 创建原生对象
            instance = clazz.newInstance();

            this.factoryBeanObjectCache.put(beanName, instance);

            // 不为空则封装成BeanWrapper
            BeanWrapper beanWrapper = new BeanWrapper(instance);
            // 在创建代理类前完成依赖注入
            populateBean(beanName, beanDefinition, beanWrapper);

            // AOP切面表达式匹配
            AdviceSupport config = InstantiateAopConfig(beanDefinition);

            config.setTargetClass(clazz);
            config.setTarget(instance);

            // 如果满足切面匹配规则则生成代理类
            if (config.pointCutMatch()) {
                instance = new JdkDynamicAopProxy(config).getProxy();
            }

            this.factoryBeanObjectCache.put(beanName, instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    // 实例化 AOP 配置
    private AdviceSupport InstantiateAopConfig(BeanDefinition beanDefinition) {
        AopConfig config = new AopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        return new AdviceSupport(config);
    }

    public Object getBean(Class className) throws ClassNotFoundException {
        return getBean(className.getName());
    }

    /**
     * 判断IoC容器是否为空
     *
     * @return int
     */
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[0]);
    }

    public Properties getConfig() {
        return this.reader.getConfig();
    }
}
