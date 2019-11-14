package com.lake.viocedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.lake.speechdemo.view.BaseBottomSheetView;

/**
 * 语音模块使用demo
 */
public class MainActivity extends AppCompatActivity{
    private ImageView btn;
    //权限相关
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static String[] PERMISSIONS_GROUP = {
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.speechBtn);
        btn.setOnClickListener(v->{
            //语音交互界面
            BaseBottomSheetView baseBottomSheetView = new BaseBottomSheetView(this);
            baseBottomSheetView.show();
        });
        checkPermissions();
    }

    /**
     * 检测权限环境
     */
    private void checkPermissions() {
        try {
            boolean request = false;
            for (String s : PERMISSIONS_GROUP) {
                int permission = ActivityCompat.checkSelfPermission(this, s);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_GROUP, REQUEST_PERMISSIONS_CODE);
                    request = true;
                    break;
                }
            }
            if (!request)
                init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (String s : permissions)
                Log.e("lake", "onRequestPermissionsResult: " + s);
            for (int i : grantResults)
                Log.e("lake", "onRequestPermissionsResult: " + i);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == 0)
                init();
            else
                Toast.makeText(this, "没有相关权限，demo无法运行！", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 初始化
     */
    private void init() {

    }
}
