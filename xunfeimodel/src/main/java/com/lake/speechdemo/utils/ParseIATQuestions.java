package com.lake.speechdemo.utils;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.lake.speechdemo.bean.IATResults;
import java.util.List;

/**
 * 解析听写结果
 */
public class ParseIATQuestions {
    //解析单个段落
    public static String parseBeanToStr(IATResults iatResults) {
        String str = "";
        if (iatResults != null) {
            List<IATResults.WsBean> ws = iatResults.getWs();
            if (ws != null && ws.size() > 0) {
                for (IATResults.WsBean wsBean : ws) {
                    List<IATResults.WsBean.CwBean> cwBeans = wsBean.getCw();
                    if (cwBeans != null && cwBeans.size() > 0) {
                        String cwStr = "";
                        IATResults.WsBean.CwBean cw = cwBeans.get(0);
                        cwStr = cw.getW();
                        for (IATResults.WsBean.CwBean cwBean : cwBeans) {
                            if (cwBean.getSc() > cw.getSc()) {
                                cwStr = cwBean.getW();
                            }
                        }
                        if (!TextUtils.isEmpty(cwStr)) {
                            str += cwStr;
                        }
                    }
                }
            }
        }
        return str;
    }

    //解析多个段落
    public static String parseBeanArrayToStr(IATResults[] iatResults) {
        StringBuffer stringBuffer = new StringBuffer();
        if (iatResults != null && iatResults.length > 0) {
            for (IATResults iatResult : iatResults) {
                stringBuffer.append(parseBeanToStr(iatResult));
            }
        }
        return stringBuffer.toString();
    }

    //解析json为实体类
    public static IATResults parseStrToBean(String json) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, IATResults.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
