package com.zjl.log.codeless

class LogTransformHelper {

    Object extension

    boolean disableJar
    boolean lambdaEnabled
    boolean useInclude
    HashSet<String> exclude = ['android.support',
                               'android.arch',
                               'androidx']
    HashSet<String> include = ['butterknife.internal.DebouncingOnClickListener',
                               'com.jakewharton.rxbinding.view.ViewClickOnSubscribe',
                               'com.facebook.react.uimanager.NativeViewHierarchyManager']

    LogTransformHelper(Object extension) {
        this.extension = extension
    }

    void onTransform() {
        useInclude = extension.useInclude
        disableJar = extension.disableJar
        lambdaEnabled = extension.lambdaEnabled
        HashSet<String> excludePackages = extension.exclude
        if (excludePackages != null) {
            exclude.addAll(excludePackages)
        }
        HashSet<String> includePackages = extension.include
        if (includePackages != null) {
            include.addAll(includePackages)
        }
    }


    ClassAnalysis analyze(String className) {

        ClassAnalysis analysis = new ClassAnalysis(className)

        if (!analysis.isAndroidGenerated()) {
            if (useInclude) {
                analysis.isShouldModify = false
                for (pkgName in include) {
                    if (className.startsWith(pkgName)) {
                        analysis.isShouldModify = true
                        break
                    }
                }
            } else {
                analysis.isShouldModify = true
                if (!analysis.isViewPager()) {
                    for (pkgName in exclude) {
                        if (className.startsWith(pkgName)) {
                            analysis.isShouldModify = false
                            break
                        }
                    }
                }
            }
        }
        return analysis
    }
}

