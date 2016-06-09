package com.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来获取修饰该注解的类信息
 * Created by lizhaoxuan on 16/5/21.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GetMsg {
    int id();
    String name() default "default";
}
