package com.zjl.log.codeless.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用此注解通知SDK某方法是控件的点击回调方法。
 * <ol>
 * <li>仅用于SDK不能正确判断点击回调方法的场景，如通过xml指定回调方法等
 * <li>方法签名必须如 '(Landroid/view/View;)V'
 * </ol>
 * Created by zjl on 2018/12/27
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnClickView {
}
