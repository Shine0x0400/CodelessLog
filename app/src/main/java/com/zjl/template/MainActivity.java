package com.zjl.template;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;

import com.zjl.log.codeless.annotation.AutoLogEvent;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener
        , DialogInterface.OnClickListener
        , AdapterView.OnItemClickListener
        , AdapterView.OnItemSelectedListener
        , RatingBar.OnRatingBarChangeListener
        , SeekBar.OnSeekBarChangeListener
        , CompoundButton.OnCheckedChangeListener
        , RadioGroup.OnCheckedChangeListener
        , ExpandableListView.OnGroupClickListener
        , ExpandableListView.OnChildClickListener {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_clickable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doTaskA();

                startActivity(new Intent(MainActivity.this, PagerActivity.class));
            }
        });

        findViewById(R.id.tv_jump_recycler).setOnClickListener(this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                Log.i("MainActivity", "click dialog positive");
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Log.i("MainActivity", "click dialog negative");
                return;
            case DialogInterface.BUTTON_NEUTRAL:
                Log.i("MainActivity", "click dialog neutral");
                break;
        }
    }

    @Override
    public void onClick(View v) {
//        Toast.makeText(this, "hello aha", Toast.LENGTH_LONG).show();
        switch (v.getId()) {
            case R.id.tv_jump_recycler:
                startActivity(new Intent(MainActivity.this, RecyclerViewActivity.class));
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // not injected
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        return false;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // not injected
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // not injected
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @AutoLogEvent(eventName = "taskA", properties = "{\"isSuccess\":true}")
    private void doTaskA() {
        Log.d(TAG, "doing taskA");
    }
}
