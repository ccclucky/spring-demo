package com.cclucky.spring.beans;

public class BeanWrapper {

    private final Object wrapperInstance;
    private final Class<?> wrapperClass;

    public BeanWrapper(Object instance) {
        this.wrapperInstance = instance;
        this.wrapperClass = this.wrapperInstance.getClass();
    }

    public Object getWrapperInstance() {
        return wrapperInstance;
    }

    public Class<?> getWrapperClass() {
        return wrapperClass;
    }
}
