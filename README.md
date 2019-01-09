[TOC]
# 简介
这是一款帮助Android项目实现无痕埋点的gradle插件，无需写任何代码就可自动采集控件点击和页面进出等事件。

本项目借助Transform API、ASM等AOP技术实现。

# 接入
1. 在project级别的build.gradle文件，添加**无痕埋点编译插件**依赖：
> 目前该插件还未上传任何maven仓库，请跳过该步骤，而且跑本demo也不需要该步骤。

```
buildscript {
    repositories {
        maven { url 'http://maven.zjl.com/nexus/content/repositories/releases/' }
        maven { url 'http://maven.zjl.com/nexus/content/repositories/snapshots/' }
    }

    dependencies {
        classpath 'com.zjl:log-gradle-plugin:x.x.x'
    }
}
```
2. 在app级别build.gradle文件中应用插件：

```
import com.zjl.log.codeless.LogPlugin

apply plugin: LogPlugin
// 无痕埋点配置
CodelessLog {
    // 控制是否打印插桩日志
    debug = true

    // 控制是否插桩jar文件
    disableJar = false

    // 是否插桩lambda表达式
    lambdaEnabled = true

    /**
     * 控制可以插桩类的范围，有include和exclude两种模式。
     * 通过useInclude控制使用include模式还是exclude模式。
     * include和exclude是数组类型，存放类完整路径前缀。
     */
    exclude = []
    useInclude = true
    include = [
            'com.zjl.template',
            'com.zjl'
    ]

}
```
3. 插桩开关，在project的gradle.properties中添加：
```
# 开关无痕埋点注入过程
disableCodelessPlugin=false
```
4. 配置proguard

```
# 无痕埋点
-keep class com.zjl.log.**{*;}
```

5. 经过以上步骤，就可以打出自动埋点的包了。

# 控件点击
## 可采集的点击
插件会自动识别以下接口的实现类，并在相应方法中插桩埋点代码。
```java
View.OnClickListener
DialogInterface.OnClickListener
AdapterView.OnItemClickListener
AdapterView.OnItemSelectedListener
RatingBar.OnRatingBarChangeListener
SeekBar.OnSeekBarChangeListener
CompoundButton.OnCheckedChangeListener
RadioGroup.OnCheckedChangeListener
ExpandableListView.OnGroupClickListener
ExpandableListView.OnChildClickListener
```
具体的接口方法是：

```java
onClick(Landroid/view/View;)V
onClick(Landroid/content/DialogInterface;I)V
onItemClick(Landroid/widget/AdapterView;Landroid/view/View;IJ)V
onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V
onGroupClick(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z
onChildClick(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z
onRatingChanged(Landroid/widget/RatingBar;FZ)V
onStopTrackingTouch(Landroid/widget/SeekBar;)V
onCheckedChanged(Landroid/widget/CompoundButton;Z)V
onCheckedChanged(Landroid/widget/RadioGroup;I)V
onTabChanged(Ljava/lang/String;)V
onTabSelected(Landroid/support/design/widget/TabLayout$Tab;)V
onTabSelected(Lcom/google/android/material/tabs/TabLayout$Tab;)V
```

如果某控件的点击回调不是上述方法，或插件未能正确识别回调方法的时候，可以通过@**OnClickView**注解，主动指定为回调方法，插件会在有该注解的方法内插桩控件点击埋点。
## 日志内容
这是一个栗子：
```
$element_path:"android.widget.LinearLayout[0]/android.widget.FrameLayout[0]/android.support.v7.widget.FitWindowsLinearLayout[0]/android.support.v7.widget.ContentFrameLayout[0]/android.widget.LinearLayout[0]/android.widget.RelativeLayout[0]/android.support.v7.widget.AppCompatTextView[0]"
$element_id:"tv_click_me"
$screen_name:"com.zjl.codeless.TestActivity"
$screen_title:"测试页面"
$element_content:"点我试试"
$element_type:"TextView"
```
字段说明：
- $element_path：从页面的root view到该控件的遍历路径，[n]表示**同类型**的兄弟节点中的第几个，如"TextView[1]"表示该控件是其父布局中的第二个TextView。
- $element_id：为控件设置的id，如 "@+id/tv_clickable"
- $element_type：控件类型
- $element_content：控件上显示的文本
- $element_position：ListView等AdapterView，该控件对应的adapter position
- $screen_name：所在页面的完整类名
- $screen_title：所在页面标题栏处当前显示的文本

# 页面进入退出
> 对于Activity页面，可以通过Application.registerActivityLifecycleCallbacks方法很容易的收集到进入退出事件，对于Fragment页面就没有如此便捷的回调方法了，还得借助插桩实现。

插件会自动插桩以下Fragment的方法，实现页面事件采集：

```java
onResume()V
onPause()V
setUserVisibleHint(Z)V
onHiddenChanged(Z)V
```


# 自定义事件
如果想在某个方法执行时自动上报，可以使用@AutoLogEvent注解，如：

```java
@AutoLogEvent(eventName = "taskA", properties = "{\"isSuccess\":true}")
private void doTaskA() {
    Log.d(TAG, "doing taskA");
}
```
# 额外信息
通过实现informative包下的接口，可以为自动采集提供额外的信息。
## 接口TrackableAdapter

```java
/**
 * 若要采集 {@link android.widget.AdapterView} 如ListView、GridView等，点击的额外属性，令其adapter实现该接口。
 * Created by zjl on 2018/12/28.
 */
public interface TrackableAdapter {
    /**
     * 点击 position 处 item 的扩展属性
     */
    JSONObject getItemTrackProperties(int position) throws JSONException;
}
```
## 接口TrackableExpandableListAdapter

```java
/**
 * 若要采集 {@link android.widget.ExpandableListView} 点击的额外属性，令其adapter实现该接口。
 * Created by zjl on 2018/12/28.
 */
public interface TrackableExpandableListAdapter {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     */
    JSONObject getChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     */
    JSONObject getGroupItemTrackProperties(int groupPosition) throws JSONException;
}
```

# 忽略采集
如果想忽略某些类、某些方法的自动采集，可以通过添加注解实现，有以下注解可以使用：

注解| 作用对象 | 功能
---|---|---
@NonInjection| 方法 | 被注解对象不会被插桩
@IgnoreAutoTrackElementEvent | Activity | 忽略该页面的控件点击事件
@IgnoreAutoTrackPageEvent | Fragment | 忽略该页面的进入退出事件
@IgnoreAutoTrackPageAndElementEvent | Activity | 忽略该页面的控件点击和页面进出事件

# 鸣谢
https://github.com/sensorsdata
