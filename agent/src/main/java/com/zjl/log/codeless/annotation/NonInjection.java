
package com.zjl.log.codeless.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 若不希望SDK插桩，用此注解通知SDK不要进行插桩操作。
 * Created by zjl on 2018/12/27
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonInjection {
}
