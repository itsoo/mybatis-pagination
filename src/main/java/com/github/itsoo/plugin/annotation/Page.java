package com.github.itsoo.plugin.annotation;

import java.lang.annotation.*;

/**
 * 分页注解
 *
 * @author zxy
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Page {

    String description() default "分页注解";
}
