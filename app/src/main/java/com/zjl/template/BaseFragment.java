package com.zjl.template;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * Created by zjl on 2019/1/9.
 */
public abstract class BaseFragment extends Fragment {
    public static final String ARG_PRINT = "ARG_PRINT";

    private static final String TAG = "BaseFragment";

    private boolean printLog = false;

    public BaseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            printLog = arguments.getBoolean(ARG_PRINT);
        }
    }

    @Override
    public void onResume() {
        if (printLog) {
            Log.d(TAG, "onResume() called");
        }
        super.onResume();

        boolean isResumed = isResumed();
        boolean userVisibleHint = getUserVisibleHint();
        boolean hidden = isHidden();
        if (printLog) {
            Log.d(TAG, String.format("onResume: isResumed=%1$b, userVisibleHint=%2$b, hidden=%3$b, ", isResumed, userVisibleHint, hidden));
        }

        if (userVisibleHint && !hidden) {
            onVisibilityChanged(true);
        }
    }

    @Override
    public void onPause() {
        if (printLog) {
            Log.d(TAG, "onPause() called");
        }
        super.onPause();

        boolean isResumed = isResumed();
        boolean userVisibleHint = getUserVisibleHint();
        boolean hidden = isHidden();
        if (printLog) {
            Log.d(TAG, String.format("onPause: isResumed=%1$b, userVisibleHint=%2$b, hidden=%3$b, ", isResumed, userVisibleHint, hidden));
        }

        if (userVisibleHint && !hidden) {
            onVisibilityChanged(false);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (printLog) {
            Log.d(TAG, "onHiddenChanged() called with: hidden = [" + hidden + "]");
        }
        super.onHiddenChanged(hidden);

        boolean isResumed = isResumed();
        boolean userVisibleHint = getUserVisibleHint();
        if (printLog) {
            Log.d(TAG, String.format("onHiddenChanged: isResumed=%1$b, userVisibleHint=%2$b, hidden=%3$b, ", isResumed, userVisibleHint, hidden));
        }

        if (isResumed && userVisibleHint) {
            onVisibilityChanged(hidden);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (printLog) {
            Log.d(TAG, "setUserVisibleHint() called with: isVisibleToUser = [" + isVisibleToUser + "]");
        }
        super.setUserVisibleHint(isVisibleToUser);

        boolean isResumed = isResumed();
        boolean userVisibleHint = getUserVisibleHint();
        boolean hidden = isHidden();
        if (printLog) {
            Log.d(TAG, String.format("setUserVisibleHint: isResumed=%1$b, userVisibleHint=%2$b, hidden=%3$b, ", isResumed, userVisibleHint, hidden));
        }

        if (isResumed && !hidden) {
            onVisibilityChanged(isVisibleToUser);
        }
    }

    protected void onVisibilityChanged(boolean visible) {
        if (printLog) {
            Log.d(TAG, "onVisibilityChanged() called with: visible = [" + visible + "]");
        }
    }
}
