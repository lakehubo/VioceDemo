package com.lake.speechdemo.utils;
import java.util.Calendar;
import java.util.Date;

public class TimeConvertUtils {
    public static double convertTimeToSec(String time) {
        if ((time.split(":")).length != 3) {
            return 0;
        }
        int hour = Integer.parseInt((time.split(":"))[0]);
        int min = Integer.parseInt((time.split(":"))[1]);
        int sec = Integer.parseInt((time.split(":"))[2]);
        return (double) hour * 60 * 60 + (double) min * 60 + (double) sec;
    }

    //获取当天0点时间戳
    public static long getTimesMorningByTime(long time) {
        Calendar cal = getLocalTime(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        time = cal.getTimeInMillis() / 1000;
        return (long) (time);
    }

    //比较时间差 秒数
    public static long difftime(long timeNow, long timeTmp) {
        return timeNow - timeTmp;
    }

    //获取当前日历
    public static Calendar getLocalTime(long time) {
        Date date = new Date(time * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
}
