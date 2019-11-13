package com.lake.viocedemo;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.lake.speechdemo.utils.Constants;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //讯飞语音初始化
        // 设置使用v5+
        String param = ("appid=" + Constants.APP_ID);
        param += ",";
        param += SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC;
        SpeechUtility.createUtility(this, param);//sdk初始化
    }
}
