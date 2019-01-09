package com.zjl.log.codeless

/**
 * 单个类的处理选项
 */
class ClassAnalysis {

    public String className

    boolean isShouldModify = false

    ClassAnalysis(String className) {
        this.className = className
    }

    boolean isViewPager() {
        return className == 'android.support.v4.view.ViewPager' || className == 'androidx.viewpager.widget.ViewPager'
    }

    boolean isAndroidGenerated() {
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }

}