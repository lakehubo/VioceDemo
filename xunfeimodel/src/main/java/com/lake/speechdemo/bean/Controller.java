package com.lake.speechdemo.bean;

import java.util.Map;

public class Controller {
    private Map<String,Integer> deviceControlMap;

    public Controller(Map<String, Integer> deviceControlMap) {
        this.deviceControlMap = deviceControlMap;
    }

    public Map<String, Integer> getDeviceControlMap() {
        return deviceControlMap;
    }

    public void setDeviceControlMap(Map<String, Integer> deviceControlMap) {
        this.deviceControlMap = deviceControlMap;
    }
}
