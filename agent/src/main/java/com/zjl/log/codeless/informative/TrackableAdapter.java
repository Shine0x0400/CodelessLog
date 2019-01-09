package com.zjl.log.codeless.informative;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 若要采集 {@link android.widget.AdapterView} 如ListView、GridView等，点击的额外属性，令其adapter实现该接口。
 * Created by zjl on 2018/12/28.
 */
public interface TrackableAdapter {
    /**
     * 点击 position 处 item 的扩展属性
     *
     * @param position 当前 item 所在位置
     * @return JSONObject
     * @throws JSONException JSON 异常
     */
    JSONObject getItemTrackProperties(int position) throws JSONException;
}
