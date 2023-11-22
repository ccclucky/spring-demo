package com.cclucky.mcvframework.annotation;

import java.lang.annotation.*;

/**
 * xiang 2018/4/21
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String value() default  "";
}