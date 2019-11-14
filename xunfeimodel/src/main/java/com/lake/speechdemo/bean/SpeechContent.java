package com.lake.speechdemo.bean;

import java.util.List;

/**
 * 语音交互内容实体
 */
public class SpeechContent {
    private int type;//0问题 1回答
    private String tiltle;//标题
    private List<Controller> controllerList;//操作集合

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTiltle() {
        return tiltle;
    }

    public void setTiltle(String tiltle) {
        this.tiltle = tiltle;
    }

    public List<Controller> getControllerList() {
        return controllerList;
    }

    public void setControllerList(List<Controller> controllerList) {
        this.controllerList = controllerList;
    }
}
