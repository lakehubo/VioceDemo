package com.lake.speechdemo.utils;

import android.os.Environment;
import java.io.File;

//常量
public class Constants {
    public static final String APP_ID = "5c480f2a";//科大讯飞ID
    public static final String Audio = new File(Environment.getExternalStorageDirectory(), "XunFei").getPath() + "/audio";
}
