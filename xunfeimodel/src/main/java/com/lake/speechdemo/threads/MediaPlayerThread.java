package com.lake.speechdemo.threads;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import com.lake.speechdemo.bean.MediaResult;
import com.lake.speechdemo.interfaces.MediaPlayerListener;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaPlayerThread extends Thread {
    private MediaPlayer mediaPlayer;
    private boolean stop = false;//线程开关
    private MediaPlayerListener listener;
    private int type = 0;
    private BlockingQueue<MediaResult> playList = new LinkedBlockingQueue<>(20);

    public MediaPlayerThread(MediaPlayerListener mediaPlayerListener) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

        listener = mediaPlayerListener;
        mediaPlayer.setOnPreparedListener((MediaPlayer mp) -> {
//            mediaPlayer = mp;
            mp.start();
            listener.isPlaying();
        });
        mediaPlayer.setOnCompletionListener((MediaPlayer mp) -> {
//            mediaPlayer = mp;
            mp.stop();
            listener.isComplete();
            if (type == 1) {
                listener.isOpenListening();
            }
        });
    }

    public boolean isOpen() {
        return !stop;
    }

    public synchronized void putAudioInQueue(MediaResult mediaResult) {
        Log.e("audioTrack", "putAudioInQueue: " + mediaResult.path);
        playList.offer(mediaResult);
    }

    private synchronized MediaResult getAudioQueue() throws Exception {
        if (playList != null && playList.size() > 0) {
            return playList.take();
        }
        return null;
    }

    //重置播放器并清空播放列表
    public void resetPlayer() {
        playList.clear();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
    }

    //停止播放
    public void stopPlayer() {
        playList.clear();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        listener.isComplete();
    }

    @Override
    public void run() {
        super.run();
        while (!stop) {
            try {
                MediaResult mediaResult = getAudioQueue();
                if (mediaResult == null) {
                    sleep(160);
                    continue;
                }
                String path = mediaResult.path;
                type = mediaResult.type;
                if (TextUtils.isEmpty(path)) {
                    sleep(160);
                    continue;
                }
                File file = new File(path); //原始wav文件
                if (file.exists()) {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(path);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mediaPlayer.prepare();
                } else {
                    sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("mediaPlayer", "run: " + e.getMessage());
            }
        }
        if (mediaPlayer != null)
            mediaPlayer.stop();
    }


    public void doStop() {
        stop = true;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
