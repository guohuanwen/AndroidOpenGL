package com.bigwen.opengl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ListView;

import com.bigwen.opengl.camera.CameraActivity;
import com.bigwen.opengl.gl.OpenGLActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bigwen on 2020/9/6.
 */
public class MainActivity extends Activity {

    private ListView listView;
    private ListItemAdapter listItemAdapter;
    private Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        mActivity = this;
        listView = findViewById(R.id.list_view);
        List<String> dataList = new ArrayList<>();
        dataList.add("OpenGL");
        dataList.add("Camera");
        listItemAdapter = new ListItemAdapter(this);
        listView.setAdapter(listItemAdapter);
        listItemAdapter.setObjects(dataList);
        listItemAdapter.setItemCallback(new ListItemAdapter.ItemCallback() {
            @Override
            public void onClick(String data) {
                if ("OpenGL".equals(data)) {
                    startActivity(new Intent(mActivity, OpenGLActivity.class));
                } else if ("Camera".equals(data)) {
                    startActivity(new Intent(mActivity, CameraActivity.class));
                }
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                String[] permissionNeeded = {
                        "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO"};
                ActivityCompat.requestPermissions(this, permissionNeeded, 101);
            }
        }
    }
}
