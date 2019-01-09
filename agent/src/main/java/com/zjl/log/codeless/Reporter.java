package com.zjl.log.codeless;

import com.zjl.log.codeless.util.DebugLogger;

import org.json.JSONObject;

/**
 * 日志上报器，在此实现日志处理逻辑，比如上报服务器、暂存本地等。
 * Created by zjl on 2018/12/28.
 */
public final class Reporter {
    private static volatile Reporter sInstance;

    private Reporter() {
    }

    public static Reporter getInstance() {
        if (sInstance == null) {
            synchronized (Reporter.class) {
                if (sInstance == null) {
                    sInstance = new Reporter();
                }
            }
        }
        return sInstance;
    }

    /**
     * 上报控件点击
     *
     * @param properties 事件属性
     */
    public void reportElementClick(JSONObject properties) {
        DebugLogger.d("Report--reportElementClick", properties.toString());
    }


    /**
     * 上报通用事件
     *
     * @param eventName  事件名
     * @param properties 事件属性
     */
    public void reportEvent(String eventName, JSONObject properties) {
        DebugLogger.d("Report--reportEvent", properties.toString());
    }

    /**
     * 上报页面进入
     *
     * @param properties 事件属性
     */
    public void reportPageEnter(JSONObject properties) {
        DebugLogger.d("Report--reportPageEnter", properties.toString());
    }

    /**
     * 上报页面退出
     *
     * @param properties 事件属性
     */
    public void reportPageExit(JSONObject properties) {
        DebugLogger.d("Report--reportPageExit", properties.toString());
    }
}
