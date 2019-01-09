package com.zjl.template;

import android.app.Application;

import com.zjl.log.codeless.AutoLogConfig;
import com.zjl.log.codeless.util.DebugLogger;

/**
 * Created by zjl on 2018/12/29.
 */
public class TemplateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initLog();
    }

    private void initLog() {
        DebugLogger.config(true);
        AutoLogConfig.sharedInstance().switchFragmentPageEventAutoTrack(true);
        AutoLogConfig.sharedInstance().switchAutoTrack(true);
    }
}
