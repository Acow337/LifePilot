package com.hmdp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminActionLog {
    String module();

    String action();

    String targetType();

    /**
     * SpEL 表达式，例如 #userId、#shop.id
     */
    String targetId() default "";
}
