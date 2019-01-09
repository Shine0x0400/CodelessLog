package com.zjl.log.codeless.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 插桩标记，仅限SDK内部使用，用户禁用。
 * Created by zjl on 2018/12/27
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodelessInstrumented {
}
