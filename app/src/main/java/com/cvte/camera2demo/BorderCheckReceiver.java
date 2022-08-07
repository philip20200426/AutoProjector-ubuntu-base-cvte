package com.cvte.camera2demo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.UEventObserver;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.camera2demo.util.AutoFocusUtil;
import com.cvte.camera2demo.util.ToastUtil;

import static android.security.KeyStore.getApplicationContext;

public class BorderCheckReceiver {

    private static BorderCheckReceiver borderProtection;
    private static final int MSG_DISPLAY_TOAST_NO_BORDER = 0;
    private static final int MSG_DISPLAY_TOAST_INNER_BORDER = 1;
    private static final int MSG_DISPLAY_TOAST_OUTER_BORDER = 2;
    private static final int MSG_DISPLAY_TOAST_BORDER_BACK_FINISHED = 3;
    private static final int EVENT_DEFAULT = 0;
    private static final int EVENT_INNER_BORDER = 1;
    private static final int EVENT_OUTER_BORDER = 2;
    private static final int EVENT_BACK_FINISHED = 3;
    private static final int EVENT_NO_BORDER_FINISHED = 6;

    private static final int STEP_BORDER_SUBSCRIPT = 0;
    private static final int STEP_NUM_SUBSCRIPT = 1;

    private BorderCheckReceiver () {

    }

    public static BorderCheckReceiver getInstance() {
        synchronized (BorderCheckReceiver.class) {
            if (borderProtection == null) {
                borderProtection = new BorderCheckReceiver();
            }
        }
        return borderProtection;
    }

    public void startObserving() {
        Log.d("HBK-U","startObserving");
        BorderUEventObserver.startObserving("DEVPATH=/devices/platform/customer-AFmotor");
    }

    private UEventObserver BorderUEventObserver = new UEventObserver() {

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            String state = event.get("MOTOR_STATE");
            Log.d("HBK-U","onUEvent: "+state);
            //异常超时，结束兼听，不再更改状态机状态
            if(SystemPropertiesAdapter.get("persist.begin.take.photo", "0").equals("999")){
                return;
            }

            String[] borderUEvent = state.split("_");
            if(borderUEvent[STEP_BORDER_SUBSCRIPT] == null || borderUEvent[STEP_NUM_SUBSCRIPT] == null){
                return;
            }

            int borderType = Integer.parseInt(borderUEvent[STEP_BORDER_SUBSCRIPT]);
            switch (borderType) {
                case EVENT_NO_BORDER_FINISHED: {
                    Log.d("HBK-U","Receive EVENT_NO_BORDER_FINISHED");
                    SystemPropertiesAdapter.set("persist.begin.take.photo","2");
                    break;
                }
                case EVENT_INNER_BORDER: {
                    Log.d("HBK-U","Receive INNER_BORDER");
//                    mHandler.removeMessages(MSG_DISPLAY_TOAST_INNER_BORDER);
//                    mHandler.sendEmptyMessage(MSG_DISPLAY_TOAST_INNER_BORDER);
                    break;
                }
                case EVENT_OUTER_BORDER: {
                    Log.d("HBK-U","Receive OUTER_BORDER");
//                    mHandler.removeMessages(MSG_DISPLAY_TOAST_OUTER_BORDER);
//                    mHandler.sendEmptyMessage(MSG_DISPLAY_TOAST_OUTER_BORDER);
                    break;
                }
                case EVENT_BACK_FINISHED: {
                    SystemPropertiesAdapter.set("persist.begin.take.photo","0");
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
                    break;
                }
                default:{
                    break;
                }

            }
        };
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();
            switch (msg.what) {
                case MSG_DISPLAY_TOAST_INNER_BORDER:
                    ToastUtil.showToast(context, R.string.toast_border_check_inner);
                    break;
                case MSG_DISPLAY_TOAST_OUTER_BORDER:
                    ToastUtil.showToast(context,R.string.toast_border_check_outer);
                    break;
                case MSG_DISPLAY_TOAST_BORDER_BACK_FINISHED:
                    ToastUtil.showToast(context,  R.string.toast_border_check_back_finished);
                    break;
                default:
                    break;
            }
        }
    };
}
