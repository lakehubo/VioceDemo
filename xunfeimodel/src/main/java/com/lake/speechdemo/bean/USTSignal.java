package com.lake.speechdemo.bean;

/**
 * Created by Administrator on 2018/12/24.
 */

public class USTSignal {
    public String ust_task_text;
    //public String findsomeone_result_json;
//    public String ust_result_text;

    public USTbean usTbean;

    public String IFlyTek_aiui_result_json;

    public boolean is_attend_task;
    public String attend_result_json;//打卡时触发的语义理解，包含性别，年龄等信息，待扩展
    //int egine_type;//0:薄言语义理解//1:薄言找人
    //int answer_type;//0:sample//1:fixed sorry_not_understand//2:find_someone
    //std::string fixed_name;

    public int rec_gender;
    public int rec_age;

    public void clear() {
        ust_task_text = "";
//        ust_result_text = "";
        IFlyTek_aiui_result_json = "";
        usTbean = null;
        is_attend_task = false;
        rec_age = -1;
        rec_gender = -1;
        attend_result_json = "";
    }
}
