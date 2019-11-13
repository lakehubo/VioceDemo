package com.lake.speechdemo.bean;

public class IATResult {
    public boolean is_first = false;
    public boolean is_last = false;
    public String iat_question_string = "";

    public void clear() {
        is_first = false;
        is_last = false;
        iat_question_string = "";
    }
}
