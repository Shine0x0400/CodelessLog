
package com.zjl.log.codeless;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zjl.log.codeless.annotation.IgnoreAutoTrackPageEvent;
import com.zjl.log.codeless.annotation.TrackableTitledFragment;
import com.zjl.log.codeless.informative.TrackableAdapter;
import com.zjl.log.codeless.informative.TrackableExpandableListAdapter;
import com.zjl.log.codeless.util.AopUtil;
import com.zjl.log.codeless.util.DebugLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class AutoLogAgent {
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        boolean isDeBounceTrack = false;
        long currentOnClickTimestamp = System.currentTimeMillis();
        int hashcode = object.hashCode();
        Object targetObject = eventTimestamp.get(hashcode);
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                isDeBounceTrack = true;
            }
        }

        eventTimestamp.put(hashcode, currentOnClickTimestamp);
        return isDeBounceTrack;
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.__codeless_analytics_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.__codeless_analytics_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    private static boolean isFragment(Object object) {
        try {
            Class<?> supportFragmentClass = null;
            Class<?> androidXFragmentClass = null;
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            if (supportFragmentClass == null && androidXFragmentClass == null) {
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            if (!isFragment(object)) {
                return;
            }

            //Fragment名称
            String fragmentName = object.getClass().getName();

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            } else {
                rootView.setTag(R.id.__codeless_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackRN(Object target, int reactTag, int s, boolean b) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (!AutoLogConfig.sharedInstance().isReactNativeAutoTrackEnabled()) {
                return;
            }

            JSONObject properties = new JSONObject();
            properties.put(Constants.ELEMENT_TYPE, "RNView");
            if (target != null) {
                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
                if (resolveViewMethod != null) {
                    Object object = resolveViewMethod.invoke(target, reactTag);
                    if (object != null) {
                        View view = (View) object;
                        //获取所在的 Context
                        Context context = view.getContext();

                        //将 Context 转成 Activity
                        Activity activity = AopUtil.getActivityFromContext(context, view);
                        //$screen_name & $screen_title
                        if (activity != null) {
                            properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                            String activityTitle = AopUtil.getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put(Constants.SCREEN_TITLE, activityTitle);
                            }
                        }
                        if (view instanceof CompoundButton) {//ReactSwitch
                            return;
                        }
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            if (!TextUtils.isEmpty(textView.getText())) {
                                properties.put(Constants.ELEMENT_CONTENT, textView.getText().toString());
                            }
                        } else if (view instanceof ViewGroup) {
                            StringBuilder stringBuilder = new StringBuilder();
                            String viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                            properties.put(Constants.ELEMENT_CONTENT, viewText);
                        }
                    }
                }
            }
            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    /**
     * 跟踪Fragment进入和退出事件
     *
     * @param fragment fragment
     * @param isEnter  true：进入，false：退出
     */
    private static void trackFragmentEnterExit(Object fragment, boolean isEnter) {
        try {
            if (!AutoLogConfig.sharedInstance().isFragmentPageEventAutoTrackEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

//            Set<Integer> fragmentsSets = AutoLogConfig.sharedInstance().getAutoTrackFragments();
            boolean isAutoTrackFragment = AutoLogConfig.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
            if (!isAutoTrackFragment) {
                return;
            }

            if (fragment.getClass().getAnnotation(IgnoreAutoTrackPageEvent.class) != null
                /*|| fragmentsSets == null*/) {// TODO:ZJL 2018/12/28 fragmentsSets的逻辑再看看
                return;
            }

            JSONObject properties = new JSONObject();

            String fragmentName = fragment.getClass().getCanonicalName();
            Activity activity = null;
            try {
                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                if (getActivityMethod != null) {
                    activity = (Activity) getActivityMethod.invoke(fragment);
                }
            } catch (Exception e) {
                //ignored
            }

            if (activity != null) {
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
                properties.put(Constants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(Constants.SCREEN_NAME, fragmentName);
            }

            TrackableTitledFragment trackableTitledFragment = fragment.getClass().getAnnotation(TrackableTitledFragment.class);
            if (trackableTitledFragment != null) {
                properties.put(Constants.SCREEN_TITLE, trackableTitledFragment.title());
            }

            if (isEnter) {
                Reporter.getInstance().reportPageEnter(properties);
            } else {
                Reporter.getInstance().reportPageExit(properties);
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }


    public static void trackFragmentResume(Object object) {
        if (!AutoLogConfig.sharedInstance().isFragmentPageEventAutoTrackEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(object);
                if (parentFragment == null) {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)) {
                        trackFragmentEnterExit(object, true);
                    }
                } else {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)
                            && !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentEnterExit(object, true);
                    }
                }
            }
        } catch (Exception e) {
            //ignored
        }
    }

    public static void trackFragmentPause(Object object) {
        if (!AutoLogConfig.sharedInstance().isFragmentPageEventAutoTrackEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(object);
                if (parentFragment == null) {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)) {
                        trackFragmentEnterExit(object, false);
                    }
                } else {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)
                            && !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentEnterExit(object, false);
                    }
                }
            }
        } catch (Exception e) {
            //ignored
        }
    }

    private static boolean fragmentGetUserVisibleHint(Object fragment) {
        try {
            Method getUserVisibleHintMethod = fragment.getClass().getMethod("getUserVisibleHint");
            if (getUserVisibleHintMethod != null) {
                return (boolean) getUserVisibleHintMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    private static boolean fragmentIsHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            if (isHiddenMethod != null) {
                return (boolean) isHiddenMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (!AutoLogConfig.sharedInstance().isFragmentPageEventAutoTrackEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (fragmentIsResumed(object)) {
                if (!fragmentIsHidden(object)) {
                    trackFragmentEnterExit(object, isVisibleToUser);
                }
            }
        } else {
            // TODO:ZJL 2019/1/9  need check fragmentGetUserVisibleHint(parentFragment)?
            if (fragmentGetUserVisibleHint(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (!fragmentIsHidden(object) && !fragmentIsHidden(parentFragment)) {
                        trackFragmentEnterExit(object, isVisibleToUser);
                    }
                }
            }
        }
    }

    private static boolean fragmentIsResumed(Object fragment) {
        try {
            Method isResumedMethod = fragment.getClass().getMethod("isResumed");
            if (isResumedMethod != null) {
                return (boolean) isResumedMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!AutoLogConfig.sharedInstance().isFragmentPageEventAutoTrackEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (fragmentIsResumed(object)) {
                if (fragmentGetUserVisibleHint(object)) {
                    trackFragmentEnterExit(object, !hidden);
                }
            }
        } else {
            // TODO:ZJL 2019/1/9  need check fragmentIsHidden(parentFragment)?
            if (!fragmentIsHidden(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (fragmentGetUserVisibleHint(object) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentEnterExit(object, !hidden);
                    }
                }
            }
        }
    }

    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // ExpandableListView Type 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            // View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            // $screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            // ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            properties.put(Constants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", groupPosition));
            properties.put(Constants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(Constants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties);

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.__codeless_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof TrackableExpandableListAdapter) {
                    try {
                        TrackableExpandableListAdapter trackProperties = (TrackableExpandableListAdapter) listAdapter;
                        JSONObject jsonObject = trackProperties.getGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            AopUtil.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        DebugLogger.printStackTrace(e);
                    }
                }
            }

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, expandableListView);

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //ExpandableListView 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            properties.put(Constants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof TrackableExpandableListAdapter) {
                    TrackableExpandableListAdapter trackProperties = (TrackableExpandableListAdapter) listAdapter;
                    JSONObject jsonObject = trackProperties.getChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            //ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            properties.put(Constants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(Constants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.__codeless_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            Reporter.getInstance().reportElementClick(properties);

        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackTabHost(String tabName) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //TabHost 被忽略
            if (AopUtil.isViewIgnored(TabHost.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

            properties.put(Constants.ELEMENT_CONTENT, tabName);
            properties.put(Constants.ELEMENT_TYPE, "TabHost");

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            Class<?> supportTabLayoutCLass = null;
            Class<?> androidXTabLayoutCLass = null;
            try {
                supportTabLayoutCLass = Class.forName("android.support.design.widget.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabLayoutCLass = Class.forName("com.google.android.material.tabs.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabLayoutCLass == null && androidXTabLayoutCLass == null) {
                return;
            }

            //TabLayout 被忽略
            if (supportTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(supportTabLayoutCLass)) {
                    return;
                }
            }
            if (androidXTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(androidXTabLayoutCLass)) {
                    return;
                }
            }

            if (isDeBounceTrack(tab)) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (object instanceof Context) {
                activity = AopUtil.getActivityFromContext((Context) object, null);
            } else {
                try {
                    Field[] fields = object.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object bridgeObject = field.get(object);
                        if (bridgeObject instanceof Activity) {
                            activity = (Activity) bridgeObject;
                            break;
                        }
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }
            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            Class<?> supportTabClass = null;
            Class<?> androidXTabClass = null;
            Class<?> currentTabClass;
            try {
                supportTabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabClass = Class.forName("com.google.android.material.tabs.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabClass != null) {
                currentTabClass = supportTabClass;
            } else {
                currentTabClass = androidXTabClass;
            }

            if (currentTabClass != null) {
                Method method = null;
                try {
                    method = currentTabClass.getMethod("getText");
                } catch (NoSuchMethodException e) {
                    //ignored
                }

                if (method != null) {
                    Object text = method.invoke(tab);

                    //Content
                    if (text != null) {
                        properties.put(Constants.ELEMENT_CONTENT, text);
                    }
                }

                if (activity != null) {
                    try {
                        Field field;
                        try {
                            field = currentTabClass.getDeclaredField("mCustomView");
                        } catch (NoSuchFieldException ex) {
                            try {
                                field = currentTabClass.getDeclaredField("customView");
                            } catch (NoSuchFieldException e) {
                                field = null;
                            }
                        }

                        View view = null;
                        if (field != null) {
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                        }

                        if (view == null || view.getId() == -1) {
                            try {
                                field = currentTabClass.getDeclaredField("mParent");
                            } catch (NoSuchFieldException ex) {
                                field = currentTabClass.getDeclaredField("parent");
                            }
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                        }

                        String resourceId = activity.getResources().getResourceEntryName(view.getId());
                        if (!TextUtils.isEmpty(resourceId)) {
                            properties.put(Constants.ELEMENT_ID, resourceId);
                        }
                    } catch (Exception e) {
                        DebugLogger.printStackTrace(e);
                    }
                }
            }

            //Type
            properties.put(Constants.ELEMENT_TYPE, "TabLayout");


            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackMenuItem(MenuItem menuItem) {
        trackMenuItem(null, menuItem);
    }

    public static void trackMenuItem(Object object, MenuItem menuItem) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //MenuItem 被忽略
            if (AopUtil.isViewIgnored(MenuItem.class)) {
                return;
            }

            if (isDeBounceTrack(menuItem)) {
                return;
            }

            Context context = null;
            if (object != null) {
                if (object instanceof Context) {
                    context = (Context) object;
                }
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context != null) {
                activity = AopUtil.getActivityFromContext(context, null);
            }

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //获取View ID
            String idString = null;
            try {
                if (context != null) {
                    idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                }
            } catch (Exception e) {
                DebugLogger.printStackTrace(e);
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            //ViewID
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            //Content
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(Constants.ELEMENT_CONTENT, menuItem.getTitle());
            }

            //Type
            properties.put(Constants.ELEMENT_TYPE, "MenuItem");

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            properties.put(Constants.ELEMENT_TYPE, "RadioButton");

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (activity != null) {
                try {
                    RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(Constants.ELEMENT_CONTENT, viewText);
                            }
                        }
                        AopUtil.addViewPathProperties(activity, radioButton, properties);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.__codeless_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null) {
                return;
            }

            if (isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //Dialog 被忽略
            if (AopUtil.isViewIgnored(Dialog.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

            try {
                if (dialog.getWindow() != null) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.__codeless_analytics_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(Constants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                DebugLogger.printStackTrace(e);
            }

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            properties.put(Constants.ELEMENT_TYPE, "Dialog");

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass = null;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass == null && androidXAlertDialogClass == null) {
                return;
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(Constants.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(Constants.ELEMENT_CONTENT, object);
                            }
                        }
                    }
                }

            } else if (currentAlertDialogClass.isInstance(dialog)) {
                Button button = null;
                try {
                    Method getButtonMethod = dialog.getClass().getMethod("getButton", new Class[]{int.class});
                    if (getButtonMethod != null) {
                        button = (Button) getButtonMethod.invoke(dialog, whichButton);
                    }
                } catch (Exception e) {
                    //ignored
                }

                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(Constants.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    try {
                        Method getListViewMethod = dialog.getClass().getMethod("getListView");
                        if (getListViewMethod != null) {
                            ListView listView = (ListView) getListViewMethod.invoke(dialog);
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(Constants.ELEMENT_CONTENT, object);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        //ignored
                    }
                }
            }

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    // TODO:ZJL 2018/12/29 track recyclerview

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        try {
            //闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View Type 被忽略
            if (AopUtil.isViewIgnored(adapterView.getClass())) {
                return;
            }

            JSONObject properties = new JSONObject();

            List<Class> mIgnoredViewTypeList = AutoLogConfig.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                if (adapterView instanceof ListView) {
                    properties.put(Constants.ELEMENT_TYPE, "ListView");
                    if (AopUtil.isViewIgnored(ListView.class)) {
                        return;
                    }
                } else if (adapterView instanceof GridView) {
                    properties.put(Constants.ELEMENT_TYPE, "GridView");
                    if (AopUtil.isViewIgnored(GridView.class)) {
                        return;
                    }
                }
            }

            //ViewId
            String idString = AopUtil.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter instanceof TrackableAdapter) {
                try {
                    TrackableAdapter objectProperties = (TrackableAdapter) adapter;
                    JSONObject jsonObject = objectProperties.getItemTrackProperties(position);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    DebugLogger.printStackTrace(e);
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //Activity 名称和页面标题
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            //点击的 position
            properties.put(Constants.ELEMENT_POSITION, String.valueOf(position));

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            } else if (view instanceof TextView) {
                viewText = ((TextView) view).getText().toString();
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(Constants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.__codeless_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackDrawerOpened(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.ELEMENT_CONTENT, "Open");

            AutoLogConfig.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackDrawerClosed(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.ELEMENT_CONTENT, "Close");

            AutoLogConfig.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void trackViewOnClick(View view) {
        try {
            //关闭 AutoTrack
            if (!AutoLogConfig.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (AutoLogConfig.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.__codeless_analytics_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        return;
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }
            view.setTag(R.id.__codeless_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(Constants.ELEMENT_ID, idString);
            }

            //$screen_name & $screen_title
            if (activity != null) {
                properties.put(Constants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(Constants.SCREEN_TITLE, activityTitle);
                }
            }

            String viewType = view.getClass().getCanonicalName();

            Class<?> supportSwitchCompatClass = null;
            Class<?> androidXSwitchCompatClass = null;
            Class<?> currentSwitchCompatClass = null;
            try {
                supportSwitchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXSwitchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            if (supportSwitchCompatClass != null) {
                currentSwitchCompatClass = supportSwitchCompatClass;
            } else {
                currentSwitchCompatClass = androidXSwitchCompatClass;
            }

            Class<?> supportViewPagerClass = null;
            Class<?> androidXViewPagerClass = null;
            Class<?> currentViewPagerClass = null;
            try {
                supportViewPagerClass = Class.forName("android.support.v4.view.ViewPager");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXViewPagerClass = Class.forName("androidx.viewpager.widget.ViewPager");
            } catch (Exception e) {
                //ignored
            }

            if (supportViewPagerClass != null) {
                currentViewPagerClass = supportViewPagerClass;
            } else {
                currentViewPagerClass = androidXViewPagerClass;
            }

            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
            } else if (currentViewPagerClass != null && currentViewPagerClass.isInstance(view)) {
                viewType = "ViewPager";
                try {
                    Method getAdapterMethod = view.getClass().getMethod("getAdapter");
                    if (getAdapterMethod != null) {
                        Object viewPagerAdapter = getAdapterMethod.invoke(view);
                        Method getCurrentItemMethod = view.getClass().getMethod("getCurrentItem");
                        if (getCurrentItemMethod != null) {
                            int currentItem = (int) getCurrentItemMethod.invoke(view);
                            properties.put(Constants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", currentItem));
                            Method getPageTitleMethod = viewPagerAdapter.getClass().getMethod("getPageTitle", new Class[]{int.class});
                            if (getPageTitleMethod != null) {
                                viewText = (String) getPageTitleMethod.invoke(viewPagerAdapter, new Object[]{currentItem});
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            } else if (currentSwitchCompatClass != null && currentSwitchCompatClass.isInstance(view)) {
                viewType = "SwitchCompat";
                CompoundButton switchCompat = (CompoundButton) view;
                Method method;
                if (switchCompat.isChecked()) {
                    method = view.getClass().getMethod("getTextOn");
                } else {
                    method = view.getClass().getMethod("getTextOff");
                }
                viewText = (String) method.invoke(view);
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = "ToggleButton";
                ToggleButton toggleButton = (ToggleButton) view;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
                ImageView imageView = (ImageView) view;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else if (view instanceof RatingBar) {
                viewType = "RatingBar";
                RatingBar ratingBar = (RatingBar) view;
                viewText = String.valueOf(ratingBar.getRating());
            } else if (view instanceof SeekBar) {
                viewType = "SeekBar";
                SeekBar seekBar = (SeekBar) view;
                viewText = String.valueOf(seekBar.getProgress());
            } else if (view instanceof Spinner) {
                viewType = "Spinner";
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }

            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(Constants.ELEMENT_CONTENT, viewText.toString());
            }

            //$element_type
            properties.put(Constants.ELEMENT_TYPE, viewType);

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.__codeless_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            Reporter.getInstance().reportElementClick(properties);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static void track(String eventName, String properties) {
        try {
            if (TextUtils.isEmpty(eventName)) {
                return;
            }
            JSONObject pro = null;
            if (!TextUtils.isEmpty(properties)) {
                try {
                    pro = new JSONObject(properties);
                } catch (Exception e) {
                    DebugLogger.printStackTrace(e);
                }
            }

            Reporter.getInstance().reportEvent(eventName, pro);
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }

    }

}
