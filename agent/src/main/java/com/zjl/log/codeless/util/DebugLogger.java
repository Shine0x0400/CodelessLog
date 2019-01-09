package com.zjl.log.codeless.util;

import android.util.Log;

/**
 * Created by zjl on 2018/12/28.
 */
public class DebugLogger {
    private static final String TAG = "CODELESS";
    private static boolean enabled = false;

    private DebugLogger() {
    }


    /**
     * 设置是否开启 log
     *
     * @param enable boolean
     */

    public static void config(boolean enable) {
        enabled = enable;
    }

    public static void v(String tag, String message) {
        if (enabled) {
            Log.v(TAG, "=======[ " + tag + " ]=======" + message);
        }
    }

    public static void d(String tag, String message) {
        if (enabled) {
            Log.d(TAG, "=======[ " + tag + " ]=======" + message);
        }
    }

    public static void i(String tag, String message) {
        if (enabled) {
            Log.i(TAG, "=======[ " + tag + " ]=======" + message);
        }
    }

    public static void w(String tag, String message) {
        if (enabled) {
            Log.w(TAG, "=======[ " + tag + " ]=======" + message);
        }
    }

    public static void e(String tag, String message) {
        if (enabled) {
            Log.e(TAG, "=======[ " + tag + " ]=======" + message);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     *
     * @param e Exception
     */
    public static void printStackTrace(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
    }
}
