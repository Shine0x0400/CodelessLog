package com.zjl.log.codeless.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 如果有此标识，SDK会插桩该方法，在方法执行时上报日志
 * Created by zjl on 2018/12/27
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLogEvent {
    String eventName() default "";

    String properties() default "{}";
}
