package com.zjl.log.codeless

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class LogPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        Logger.error("------------apply LogPlugin--------------")

        Object extension = project.extensions.create(
                "CodelessLog",
                LogExtension)

        boolean disablePlugin = false
        Properties properties = new Properties()
        if (project.rootProject.file('gradle.properties').exists()) {
            properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
            disablePlugin = Boolean.parseBoolean(properties.getProperty("disableCodelessPlugin", "false"))
        }

        if (!disablePlugin) {
            AppExtension android = project.extensions.getByType(AppExtension.class)
            LogTransform transform = new LogTransform(new LogTransformHelper(extension))
            android.registerTransform(transform)

            project.afterEvaluate {
                Logger.setDebug(extension.debug)
            }
        } else {
            Logger.error("------------您已关闭无痕埋点插件--------------")
        }
    }
}