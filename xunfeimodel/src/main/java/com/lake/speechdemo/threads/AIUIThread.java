package com.lake.speechdemo.threads;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.lake.speechdemo.bean.USTSignal;
import com.lake.speechdemo.interfaces.SpeechAIUIListener;
import com.lake.speechdemo.utils.AIUIAnswerAnalyse;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

/**
 * 语义解析线程 如果不需要通过讯飞平台进行语义解析，
 * 那么可以自己在本地编写关键词解析然后来进行回答或者其他操作
 */
public class AIUIThread extends Thread {
    private static final String TAG = AIUIThread.class.getName();
    private volatile boolean stop = false;
    private Context mContext;
    private SpeechAIUIListener listener;

    public boolean isOpen() {
        return !stop;
    }

    private AIUIAgent mAIUIAgent = null;
    private int mAIUIState = AIUIConstant.STATE_IDLE;
    private String mSyncSid = "";

    //////////////////////语义理解//////////////////////////////////
    private BlockingQueue<USTSignal> ust_task_list = new LinkedBlockingQueue<>(20);
    private BlockingQueue<USTSignal> ust_result_list = new LinkedBlockingQueue<>(20);

    public synchronized void write_ust_task_list(USTSignal _ust_task_signal) {
        Log.e(TAG, "write_ust_task_list: " + _ust_task_signal.ust_task_text);
        ust_task_list.offer(_ust_task_signal);

    }

    private synchronized USTSignal read_ust_task_list() {
        int size_tmp = ust_task_list.size();
        if (size_tmp <= 0) {
            return null;
        }
        //只读取最新的问题
        while (!ust_task_list.isEmpty()) {
            try {
                return ust_task_list.take();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

        }
        return null;
    }


    private synchronized void write_ust_result_list(USTSignal _ust_result_signal) {
        ust_result_list.offer(_ust_result_signal);
    }


    public synchronized USTSignal read_ust_result_list() {
        int size_tmp = ust_result_list.size();
        if (size_tmp <= 0) {
            return null;
        }
        //只读取最新的问题
        while (!ust_result_list.isEmpty()) {
            try {
                return ust_result_list.take();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return null;
    }


    public AIUIThread(Context context, SpeechAIUIListener speechAIUIListener) {
        this.mContext = context;
        listener = speechAIUIListener;
        createAgent(context);//创建aiui引擎
    }

    @Override
    public void run() {//线程执行内容
        while (!stop) {
            try {
                USTSignal ust_task_signal_tmp = read_ust_task_list();
                if (ust_task_signal_tmp != null) {
                    if (!TextUtils.isEmpty(ust_task_signal_tmp.ust_task_text)) {
                        String rec_result_all = ust_task_signal_tmp.ust_task_text;
                        startTextNlp(rec_result_all);
                    }
                } else {
                    //避免频繁消耗cpu 先睡个觉
                    sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doStop() {
        stop = true;
    }

    private void createAgent(Context context) {
        if (null == mAIUIAgent) {
            Log.i(TAG, "create aiui agent");
            mAIUIAgent = AIUIAgent.createAgent(context, getAIUIParams(context), mAIUIListener);
        }

        if (null == mAIUIAgent) {
            final String strErrorTip = "创建AIUIAgent失败！";
            Log.e(TAG, "创建AIUIAgent失败!");
        } else {
            Log.e(TAG, "AIUIAgent已创建");
        }
    }

    //获取aiui语义理解参数
    private String getAIUIParams(Context context) {
        String params = "";

        AssetManager assetManager = context.getResources().getAssets();
        try {
            InputStream ins = assetManager.open("cfg/aiui_phone.cfg");
            byte[] buffer = new byte[ins.available()];

            ins.read(buffer);
            ins.close();

            params = new String(buffer);

            JSONObject paramsJson = new JSONObject(params);

            params = paramsJson.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return params;
    }

    private synchronized void startTextNlp(String text) {
        if (null == mAIUIAgent) {
            Log.e(TAG, "AIUIAgent 为空，请先创建");
            return;
        }
        AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
        mAIUIAgent.sendMessage(wakeupMsg);
        Log.i(TAG, "start text nlp");
        listener.showStatus();
        try {
            // 在输入参数中设置tag，则对应结果中也将携带该tag，可用于关联输入输出
            String params = "data_type=text,tag=text-tag";
            byte[] textData = text.getBytes("utf-8");

            AIUIMessage write = new AIUIMessage(AIUIConstant.CMD_WRITE, 0, 0, params, textData);
            mAIUIAgent.sendMessage(write);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private AIUIListener mAIUIListener = (AIUIEvent event) -> {
        Log.i(TAG, "on event: " + event.eventType);

        switch (event.eventType) {
            case AIUIConstant.EVENT_CONNECTED_TO_SERVER:
                Log.e(TAG, "已连接服务器");
                break;

            case AIUIConstant.EVENT_SERVER_DISCONNECTED:
                Log.e(TAG, "与服务器断连");
                break;

            case AIUIConstant.EVENT_WAKEUP:
                Log.e(TAG, "进入识别状态");
                break;

            case AIUIConstant.EVENT_RESULT: {
                try {
                    JSONObject bizParamJson = new JSONObject(event.info);
                    JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
                    JSONObject params = data.getJSONObject("params");
                    JSONObject content = data.getJSONArray("content").getJSONObject(0);

                    if (content.has("cnt_id")) {
                        String cnt_id = content.getString("cnt_id");
                        String cntStr = new String(event.data.getByteArray(cnt_id), "utf-8");

                        // 获取该路会话的id，将其提供给支持人员，有助于问题排查
                        // 也可以从Json结果中看到
                        String sid = event.data.getString("sid");
                        String tag = event.data.getString("tag");

                        Log.e(TAG, "tag=" + tag);

                        // 获取从数据发送完到获取结果的耗时，单位：ms
                        // 也可以通过键名"bos_rslt"获取从开始发送数据到获取结果的耗时
                        long eosRsltTime = event.data.getLong("eos_rslt", -1);
                        Log.e(TAG, eosRsltTime + "ms");

                        if (TextUtils.isEmpty(cntStr)) {
                            return;
                        }

                        JSONObject cntJson = new JSONObject(cntStr);


                        String sub = params.optString("sub");
                        if ("nlp".equals(sub)) {
                            // 解析得到语义结果
                            String resultStr = cntJson.optString("intent");
                            USTSignal ust_task_signal_tmp = new USTSignal();
                            //写入结果
                            ust_task_signal_tmp.IFlyTek_aiui_result_json = resultStr;
//                            ust_task_signal_tmp.ust_result_text = AIUIAnswerAnalyse.getAnswerResult(resultStr).text;
                            ust_task_signal_tmp.usTbean = AIUIAnswerAnalyse.getAnswerResult(resultStr);
                            this.write_ust_result_list(ust_task_signal_tmp);
                            Log.i(TAG, resultStr);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getLocalizedMessage());
                }
                listener.isComplete();
            }
            break;

            case AIUIConstant.EVENT_ERROR: {
                Log.e(TAG, "错误: " + event.arg1 + "\n" + event.info);
                listener.onError();
            }
            break;

            case AIUIConstant.EVENT_VAD: {
                if (AIUIConstant.VAD_BOS == event.arg1) {
                    Log.e(TAG, "找到vad_bos");
                } else if (AIUIConstant.VAD_EOS == event.arg1) {
                    Log.e(TAG, "找到vad_eos");
                } else {
                    Log.e(TAG, "" + event.arg2);
                }
            }
            break;

            case AIUIConstant.EVENT_START_RECORD: {
                Log.e(TAG, "已开始录音");
            }
            break;

            case AIUIConstant.EVENT_STOP_RECORD: {
                Log.e(TAG, "已停止录音");
            }
            break;

            case AIUIConstant.EVENT_STATE: {    // 状态事件
                mAIUIState = event.arg1;

                if (AIUIConstant.STATE_IDLE == mAIUIState) {
                    // 闲置状态，AIUI未开启
                    Log.e(TAG, "STATE_IDLE");
                } else if (AIUIConstant.STATE_READY == mAIUIState) {
                    // AIUI已就绪，等待唤醒
                    Log.e(TAG, "STATE_READY");
                } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                    // AIUI工作中，可进行交互
                    Log.e(TAG, "STATE_WORKING");
                }
            }
            break;

            case AIUIConstant.EVENT_CMD_RETURN: {
                if (AIUIConstant.CMD_SYNC == event.arg1) {    // 数据同步的返回
                    int dtype = event.data.getInt("sync_dtype", -1);
                    int retCode = event.arg2;

                    switch (dtype) {
                        case AIUIConstant.SYNC_DATA_SCHEMA: {
                            if (AIUIConstant.SUCCESS == retCode) {
                                // 上传成功，记录上传会话的sid，以用于查询数据打包状态
                                // 注：上传成功并不表示数据打包成功，打包成功与否应以同步状态查询结果为准，数据只有打包成功后才能正常使用
                                mSyncSid = event.data.getString("sid");

                                // 获取上传调用时设置的自定义tag
                                String tag = event.data.getString("tag");

                                // 获取上传调用耗时，单位：ms
                                long timeSpent = event.data.getLong("time_spent", -1);
                                if (-1 != timeSpent) {
                                    Log.e(TAG, timeSpent + "ms");
                                }

                                Log.e(TAG, "上传成功，sid=" + mSyncSid + "，tag=" + tag + "，你可以试着说“打电话给刘德华”");
                            } else {
                                mSyncSid = "";
                                Log.e(TAG, "上传失败，错误码：" + retCode);
                            }
                        }
                        break;
                    }
                } else if (AIUIConstant.CMD_QUERY_SYNC_STATUS == event.arg1) {    // 数据同步状态查询的返回
                    // 获取同步类型
                    int syncType = event.data.getInt("sync_dtype", -1);
                    if (AIUIConstant.SYNC_DATA_QUERY == syncType) {
                        // 若是同步数据查询，则获取查询结果，结果中error字段为0则表示上传数据打包成功，否则为错误码
                        String result = event.data.getString("result");

                        Log.e(TAG, result);
                    }
                }
            }
            break;

            default:
                break;
        }
    };

    public void destroyAgent() {
        if (null != mAIUIAgent) {
            Log.i(TAG, "destroy aiui agent");

            mAIUIAgent.destroy();
            mAIUIAgent = null;

            Log.e(TAG, "AIUIAgent已销毁");
        } else {
            Log.e(TAG, "AIUIAgent为空");
        }
    }
}
