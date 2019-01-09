package com.zjl.log.codeless.util;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zjl.log.codeless.AutoLogConfig;
import com.zjl.log.codeless.Constants;
import com.zjl.log.codeless.R;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by zjl on 2018/12/28.
 */
public class AopUtil {

    private static int getChildIndex(ViewParent parent, View child) {
        try {
            if (parent == null || !(parent instanceof ViewGroup)) {
                return -1;
            }

            ViewGroup _parent = (ViewGroup) parent;
            final String childIdName = AopUtil.getViewId(child);

            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < _parent.getChildCount(); i++) {
                View brother = _parent.getChildAt(i);

                if (!Pathfinder.hasClassName(brother, childClassName)) {
                    continue;
                }

                String brotherIdName = AopUtil.getViewId(brother);

                if (null != childIdName && !childIdName.equals(brotherIdName)) {
                    index++;
                    continue;
                }

                if (brother == child) {
                    return index;
                }

                index++;
            }

            return -1;
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
            return -1;
        }
    }

    public static void addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null) {
                return;
            }

            if (properties == null) {
                properties = new JSONObject();
            }

            ViewParent viewParent;
            List<String> viewPath = new ArrayList<>();
            do {
                viewParent = view.getParent();
                int index = getChildIndex(viewParent, view);
//                String idString2 = AopUtil.getViewId(view);
//                if (TextUtils.isEmpty(idString2)) {
//                    viewPath.add(view.getClass().getCanonicalName() + "[" + index + "]");
//                } else {
//                    viewPath.add(view.getClass().getCanonicalName() + "[" + idString2 + "]");
//                }
                viewPath.add(view.getClass().getCanonicalName() + "[" + index + "]");
                if (viewParent instanceof ViewGroup) {
                    view = (ViewGroup) viewParent;
                }

            } while (viewParent instanceof ViewGroup);

            Collections.reverse(viewPath);
            StringBuilder stringBuffer = new StringBuilder();
            for (int i = 1; i < viewPath.size(); i++) {
                stringBuffer.append(viewPath.get(i));
                if (i != (viewPath.size() - 1)) {
                    stringBuffer.append("/");
                }
            }
            properties.put(Constants.ELEMENT_PATH, stringBuffer.toString());
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static String traverseView(StringBuilder stringBuilder, ViewGroup root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);

                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (child instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) child);
                } else {
                    if (isViewIgnored(child)) {
                        continue;
                    }

                    Class<?> switchCompatClass = null;
                    try {
                        switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
                    } catch (Exception e) {
                        //ignored
                    }

                    if (switchCompatClass == null) {
                        try {
                            switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                        } catch (Exception e) {
                            //ignored
                        }
                    }

                    CharSequence viewText = null;
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        viewText = checkBox.getText();
                    } else if (switchCompatClass != null && switchCompatClass.isInstance(child)) {
                        CompoundButton switchCompat = (CompoundButton) child;
                        Method method;
                        if (switchCompat.isChecked()) {
                            method = child.getClass().getMethod("getTextOn");
                        } else {
                            method = child.getClass().getMethod("getTextOff");
                        }
                        viewText = (String) method.invoke(child);
                    } else if (child instanceof RadioButton) {
                        RadioButton radioButton = (RadioButton) child;
                        viewText = radioButton.getText();
                    } else if (child instanceof ToggleButton) {
                        ToggleButton toggleButton = (ToggleButton) child;
                        boolean isChecked = toggleButton.isChecked();
                        if (isChecked) {
                            viewText = toggleButton.getTextOn();
                        } else {
                            viewText = toggleButton.getTextOff();
                        }
                    } else if (child instanceof Button) {
                        Button button = (Button) child;
                        viewText = button.getText();
                    } else if (child instanceof CheckedTextView) {
                        CheckedTextView textView = (CheckedTextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof ImageView) {
                        ImageView imageView = (ImageView) child;
                        if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                            viewText = imageView.getContentDescription().toString();
                        }
                    }

                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText.toString());
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
            return stringBuilder.toString();
        }
    }

    public static void getFragmentNameFromView(View view, JSONObject properties) {
        try {
            if (view != null) {
                String fragmentName = (String) view.getTag(R.id.__codeless_analytics_tag_view_fragment_name);
                String fragmentName2 = (String) view.getTag(R.id.__codeless_analytics_tag_view_fragment_name2);
                if (!TextUtils.isEmpty(fragmentName2)) {
                    fragmentName = fragmentName2;
                }
                if (!TextUtils.isEmpty(fragmentName)) {
                    String screenName = properties.optString(Constants.SCREEN_NAME);
                    if (!TextUtils.isEmpty(fragmentName)) {
                        properties.put(Constants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", screenName, fragmentName));
                    } else {
                        properties.put(Constants.SCREEN_NAME, fragmentName);
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    public static Activity getActivityFromContext(Context context, View view) {
        Activity activity = null;
        try {
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                } else {
                    if (view != null) {
                        Object object = view.getTag(R.id.__codeless_analytics_tag_view_activity);
                        if (object != null) {
                            if (object instanceof Activity) {
                                activity = (Activity) object;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
        return activity;
    }

    public static String getViewId(View view) {
        String idString = null;
        try {
            idString = (String) view.getTag(R.id.__codeless_analytics_tag_view_id);
            if (TextUtils.isEmpty(idString)) {
                if (view.getId() != View.NO_ID) {
                    idString = view.getContext().getResources().getResourceEntryName(view.getId());
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * ViewType 被忽略
     *
     * @param viewType Class
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(Class viewType) {
        try {
            if (viewType == null) {
                return true;
            }

            List<Class> mIgnoredViewTypeList = AutoLogConfig.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                for (Class clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(viewType)) {
                        return true;
                    }

                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 判断 View 是否被忽略
     *
     * @param view View
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(View view) {
        try {
            //基本校验
            if (view == null) {
                return true;
            }

            //ViewType 被忽略
            List<Class> mIgnoredViewTypeList = AutoLogConfig.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                for (Class clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(view.getClass())) {
                        return true;
                    }
                }
            }

            //View 被忽略
            if ("1".equals(view.getTag(R.id.__codeless_analytics_tag_view_ignored))) {
                return true;
            }

            return false;
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
            return true;
        }
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (activityInfo != null) {
                                if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                    activityTitle = activityInfo.loadLabel(packageManager).toString();
                                }
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
            return null;
        }
    }

    @TargetApi(11)
    private static String getToolbarTitle(Activity activity) {
        try {
            if ("com.tencent.connect.common.AssistActivity".equals(activity.getClass().getCanonicalName())) {
                if (!TextUtils.isEmpty(activity.getTitle())) {
                    return activity.getTitle().toString();
                }
                return null;
            }

            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                if (!TextUtils.isEmpty(actionBar.getTitle())) {
                    return actionBar.getTitle().toString();
                }
            } else {
                try {
                    Class<?> appCompatActivityClass = null;
                    try {
                        appCompatActivityClass = Class.forName("android.support.v7.app.AppCompatActivity");
                    } catch (Exception e) {
                        //ignored
                    }

                    if (appCompatActivityClass == null) {
                        try {
                            appCompatActivityClass = Class.forName("androidx.appcompat.app.AppCompatActivity");
                        } catch (Exception e) {
                            //ignored
                        }
                    }
                    if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                        Method method = activity.getClass().getMethod("getSupportActionBar");
                        if (method != null) {
                            Object supportActionBar = method.invoke(activity);
                            if (supportActionBar != null) {
                                method = supportActionBar.getClass().getMethod("getTitle");
                                if (method != null) {
                                    CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                                    if (charSequence != null) {
                                        return charSequence.toString();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    //ignored
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
        return null;
    }

    /**
     * 合并 JSONObject
     *
     * @param source JSONObject
     * @param dest   JSONObject
     */
    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            if (mDateFormat == null) {
                mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
                        + ".SSS", Locale.getDefault());
            }
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date) {
                    synchronized (mDateFormat) {
                        dest.put(key, mDateFormat.format((Date) value));
                    }
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception e) {
            DebugLogger.printStackTrace(e);
        }
    }

    private static SimpleDateFormat mDateFormat = null;
}
