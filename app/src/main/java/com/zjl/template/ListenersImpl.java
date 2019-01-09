package com.zjl.template;

import android.view.View;
import android.view.View.OnClickListener;

import com.zjl.log.codeless.annotation.NonInjection;

/**
 * Created by zjl on 2018/12/29.
 */
public class ListenersImpl implements OnClickListener {

    @NonInjection
    @Override
    public void onClick(View v) {
        com.zjl.log.codeless.AutoLogAgent.trackViewOnClick(v);
    }
}
