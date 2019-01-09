package com.zjl.log.codeless.informative;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 若要采集 {@link android.widget.ExpandableListView} 点击的额外属性，令其adapter实现该接口。
 * Created by zjl on 2018/12/28.
 */
public interface TrackableExpandableListAdapter {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     *
     * @param groupPosition int
     * @param childPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     *
     * @param groupPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getGroupItemTrackProperties(int groupPosition) throws JSONException;
}
