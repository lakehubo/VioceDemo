package com.lake.speechdemo.bean;


public class TTSSignal {
    //如果此内容不为空，则仅需进行此操作//存放待播放音频的路径
    public String exit_wave_path_task;//播放已存在的音频，如调节音量

    public String tts_task_text;
    public String dst_wave_path;
    public int type = 0;
    public String tts_time;
    public boolean show_text = false;
    public int wave_priority_level = WavePriorityLevel.VIDEO_CONFERENCE_LEVEL;//各种语音的优先级

    public String wave_label;//可以为具体的rec_q_id，也可以为其他标识

    //findsomeone_answer	IAT_LEVEL				合成wave到个人模板相关目录
    //stranger				WELCOME_LEVEL_SECOND	合成wave到总模板相关目录
    //rec					WELCOME_LEVEL_SECOND	合成wave到总模板相关目录
    //具体的rec_q_id：		WELCOME_LEVEL_FIRST		合成wave到个人模板相关目录

    /**
     * 清除值
     */
    public void clear() {
        exit_wave_path_task = "";
        tts_task_text = "";
        dst_wave_path = "";
        wave_label = "";
        show_text = false;
        wave_priority_level = WavePriorityLevel.VIDEO_CONFERENCE_LEVEL;
    }
}
