package com.lake.speechdemo.dapater;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lake.speechdemo.R;
import com.lake.speechdemo.bean.ContentData;

import java.util.List;

/**
 * 回答列表适配器
 */
public class VoiceContentItemAdapter extends RecyclerView.Adapter {
    private Context context;
    private List<ContentData> realData;

    public static final int TYPE_QUESTION = 0;
    public static final int TYPE_ANSWER = 1;
    public static final int TYPE_DEVICE_CAONTROL = 2;

    public VoiceContentItemAdapter(Context context, List<ContentData> mdatas) {
        this.context = context;
        this.realData = mdatas;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (getItemViewType(i) == TYPE_QUESTION) {
            View view = LayoutInflater.from(context).inflate(R.layout.question_item_layout, viewGroup, false);
            return new QuestionViewHolder(view);
        } else if (getItemViewType(i) == TYPE_ANSWER) {
            View view = LayoutInflater.from(context).inflate(R.layout.answer_item_layout, viewGroup, false);
            return new AnswerViewHolder(view);
        } else if (getItemViewType(i) == TYPE_DEVICE_CAONTROL) {
            View view = LayoutInflater.from(context).inflate(R.layout.rect_item_layout, viewGroup, false);
            return new DeviceControllerViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (getItemViewType(i) == TYPE_QUESTION) {
            onBindQuestionViewHolder((QuestionViewHolder) viewHolder, i);
        } else if (getItemViewType(i) == TYPE_ANSWER) {
            onBindAnswerViewHolder((AnswerViewHolder) viewHolder, i);
        } else if (getItemViewType(i) == TYPE_DEVICE_CAONTROL) {
            onBindDeviceControlViewHolder((DeviceControllerViewHolder) viewHolder, i);
        }
    }

    /**
     * 绑定设备控制
     *
     * @param viewHolder
     * @param i
     */
    private void onBindDeviceControlViewHolder(DeviceControllerViewHolder viewHolder, int i) {
        String name = realData.get(i).getName();
        if (TextUtils.isEmpty(name)) {
            viewHolder.setNoData();
        } else {
            viewHolder.setName(name);
            viewHolder.setSelected(false);
            viewHolder.setPosition(i);
        }
    }

    /**
     * 绑定回答
     *
     * @param viewHolder
     * @param i
     */
    private void onBindAnswerViewHolder(AnswerViewHolder viewHolder, int i) {
        viewHolder.setAnswerStr(realData.get(i).getName());
    }

    /**
     * 绑定问题
     *
     * @param viewHolder
     * @param i
     */
    private void onBindQuestionViewHolder(QuestionViewHolder viewHolder, int i) {
        viewHolder.setQuestionStr(realData.get(i).getName());
    }

    @Override
    public int getItemViewType(int position) {
        if (realData == null)
            return -1;
        return realData.get(position).getContentType();
    }


    @Override
    public int getItemCount() {
        if (realData == null)
            return 0;
        return realData.size();
    }

    /**
     * 问题
     */
    class QuestionViewHolder extends RecyclerView.ViewHolder {
        private TextView questionTv;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTv = itemView.findViewById(R.id.question_text);
        }

        public void setQuestionStr(String str) {
            questionTv.setText("“" + str + "”");
        }
    }

    /**
     * 回答标题
     */
    class AnswerViewHolder extends RecyclerView.ViewHolder {
        private TextView answerTv;

        public AnswerViewHolder(@NonNull View itemView) {
            super(itemView);
            answerTv = itemView.findViewById(R.id.answer_text);
        }

        public void setAnswerStr(String str) {
            answerTv.setText(str);
        }
    }

    /**
     * 回答携带的设备/场景操作选项
     */
    class DeviceControllerViewHolder extends RecyclerView.ViewHolder {
        private TextView cName;
        private ConstraintLayout itemBg;
        private boolean selected = false;//是否选中
        private int position = -1;

        public DeviceControllerViewHolder(@NonNull View itemView) {
            super(itemView);
            cName = itemView.findViewById(R.id.control_text);
            itemBg = itemView.findViewById(R.id.rect_item_bg);

            itemBg.setOnClickListener(V -> {
                if (TextUtils.isEmpty(cName.getText().toString()))
                    return;
                setSelected(!selected);
                if (onDeviceControlListener != null) {
                    onDeviceControlListener.onClickControl(selected, cName.getText().toString(), position);
                }
            });
        }

        public void setPosition(int i) {
            position = i;
        }

        public void setName(String str) {
            cName.setText(str);
        }

        /**
         * 是否被选中
         *
         * @param selected
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
            itemBg.setBackgroundResource(selected ? R.drawable.rect_org_item_bg : R.drawable.rect_white_item_bg);
            cName.setTextColor(selected ? Color.WHITE : Color.parseColor("#ff6e00"));
        }

        public void setNoData() {
            cName.setText("");
            itemBg.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    OnDeviceControlListener onDeviceControlListener;

    public interface OnDeviceControlListener {
        void onClickControl(boolean select, String name, int position);
    }

    public void setOnDeviceControlListener(OnDeviceControlListener onDeviceControlListener) {
        this.onDeviceControlListener = onDeviceControlListener;
    }
}
