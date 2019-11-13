package com.lake.speechdemo.utils;

import com.lake.speechdemo.bean.USTbean;

import org.json.JSONArray;
import org.json.JSONObject;

public class AIUIAnswerAnalyse {
    public static USTbean getAnswerResult(String restultJson) {
        USTbean usTbean = new USTbean();
        usTbean.text = "对不起，我不知道您在说什么？";//默认回答
        try {
            JSONObject jsonObject = new JSONObject(restultJson);
            JSONObject answer = jsonObject.optJSONObject("answer");
            String answerStr = answer.optString("text");
            usTbean.text = answerStr;
            JSONObject data = jsonObject.optJSONObject("data");
            if (data != null) {
                JSONArray results = data.optJSONArray("result");
                if (results != null && results.length() > 0) {
                    JSONObject resultMedia = results.optJSONObject(0);
                    if(resultMedia.has("playUrl")){//媒体播放
                        String palyUrl = results.optJSONObject(0).optString("playUrl");
                        usTbean.playUrl = palyUrl;
                        usTbean.text = "";
                    }else if(resultMedia.has("url")){//音频播放
                        String url = results.optJSONObject(0).optString("url");
                        String imagUrl = results.optJSONObject(0).optString("imgUrl");
                        usTbean.url = url;
                        usTbean.imgUrl = imagUrl;
                        usTbean.text = "";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usTbean;
    }
}
