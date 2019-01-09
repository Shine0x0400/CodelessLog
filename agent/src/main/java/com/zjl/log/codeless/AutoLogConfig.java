package com.zjl.log.codeless;

import android.view.View;

import com.zjl.log.codeless.annotation.IgnoreAutoTrackElementEvent;
import com.zjl.log.codeless.annotation.IgnoreAutoTrackPageAndElementEvent;
import com.zjl.log.codeless.annotation.IgnoreAutoTrackPageEvent;
import com.zjl.log.codeless.util.DebugLogger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by zjl on 2018/12/28.
 */
public final class AutoLogConfig {
    private static volatile AutoLogConfig sInstance;

    private boolean mEnableAutoTrack;
    private boolean mEnableReactNativeAutoTrack;
    private boolean mEnableFragmentPageEventAutoTrack;

    // 需要采集页面事件的Fragments
    private Set<Integer> mAutoTrackFragments;

    // 不需要采集页面事件的Activities
    private List<Integer> mAutoTrackIgnoredActivities;

    private AutoLogConfig() {
    }

    public static AutoLogConfig sharedInstance() {
        if (sInstance == null) {
            synchronized (AutoLogConfig.class) {
                if (sInstance == null) {
                    sInstance = new AutoLogConfig();
                }
            }
        }
        return sInstance;
    }

    /**
     * 开关自动收集
     *
     * @param enable 是否打开
     */
    public void switchAutoTrack(boolean enable) {
        mEnableAutoTrack = enable;
    }

    /**
     * 是否开启 AutoTrack
     *
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    public boolean isAutoTrackEnabled() {
        return mEnableAutoTrack;
    }

    /**
     * 开关React Native自动收集
     *
     * @param enable 是否打开
     */
    public void switchReactNativeAutoTrack(boolean enable) {
        this.mEnableReactNativeAutoTrack = enable;
    }

    public boolean isReactNativeAutoTrackEnabled() {
        return this.mEnableReactNativeAutoTrack;
    }

    /**
     * 是否开启自动追踪Fragment的页面进入退出事件
     * 默认不开启
     *
     * @param enable 是否打开
     */
    public void switchFragmentPageEventAutoTrack(boolean enable) {
        this.mEnableFragmentPageEventAutoTrack = enable;
    }

    public boolean isFragmentPageEventAutoTrackEnabled() {
        return this.mEnableFragmentPageEventAutoTrack;
    }


    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(IgnoreAutoTrackPageAndElementEvent.class) != null) {
            return true;
        }

        if (activity.getAnnotation(IgnoreAutoTrackElementEvent.class) != null) {
            return true;
        }

        return false;
    }

    /**
     * 指定哪些 activity 不被AutoTrack
     * <p>
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList activity列表
     */
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        int hashCode;
        for (Class<?> activity : activitiesList) {
            if (activity != null) {
                hashCode = activity.hashCode();
                if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                    mAutoTrackIgnoredActivities.add(hashCode);
                }
            }
        }
    }

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activitiesList List
     */
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode;
            for (Class activity : activitiesList) {
                if (activity != null) {
                    hashCode = activity.hashCode();
                    if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                        mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    /**
     * 指定某个 activity 不被 AutoTrack
     *
     * @param activity Activity
     */
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.add(hashCode);
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activity Class
     */
    public void resumeAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    /**
     * 判断 AutoTrack 时，某个 Fragment 的 $AppViewScreen 是否被采集
     *
     * @param fragment Fragment
     * @return 某个 Fragment 的 $AppViewScreen 是否被采集
     */
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                if (mAutoTrackFragments.contains(fragment.hashCode())
                        || mAutoTrackFragments.contains(fragment.getCanonicalName().hashCode())) {
                    return true;
                } else {
                    return false;
                }
            }

            if (fragment.getClass().getAnnotation(IgnoreAutoTrackPageEvent.class) != null) {
                return false;
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }

        return true;
    }

    /**
     * 返回设置 AutoTrack 的 Fragments 集合，如果没有设置则返回 null.
     *
     * @return Set
     */
    public Set<Integer> getAutoTrackFragments() {
        return mAutoTrackFragments;
    }


    private List<Class> mIgnoredViewTypeList = new ArrayList<>();

    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    public void ignoreViewType(Class viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains(viewType)) {
            mIgnoredViewTypeList.add(viewType);
        }
    }


    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.__codeless_analytics_tag_view_properties, properties);
    }

}
