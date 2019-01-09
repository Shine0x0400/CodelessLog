package com.zjl.template;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NormalFragment extends BaseFragment {
    public static final String ARG_TAG = "ARG_TAG";

    private int tag = -1;

    public static NormalFragment createInstance(int tag, boolean printLog) {
        NormalFragment fragment = new NormalFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAG, tag);
        args.putBoolean(ARG_PRINT, printLog);

        fragment.setArguments(args);
        return fragment;
    }


    public NormalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            tag = arguments.getInt(ARG_TAG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_normal, container, false);
        ((TextView) root.findViewById(R.id.tv_tag)).setText("" + tag);


        return root;
    }

    @Override
    protected void onVisibilityChanged(boolean visible) {
        if (visible) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.setTitle("" + tag);
            }
        }
    }
}
