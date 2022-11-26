package com.cvte.autoprojector;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UEventObserver;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.autoprojector.util.AutoFocusUtil;
import com.cvte.autoprojector.util.ImageUtil;
import com.cvte.autoprojector.util.MotorUtil;
import com.cvte.autoprojector.util.ToastUtil;

import static android.security.KeyStore.getApplicationContext;
import static com.cvte.autoprojector.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;

public class BorderCheckReceiver {

    private static BorderCheckReceiver borderProtection;
    private static final int MSG_DISPLAY_TOAST_NO_BORDER = 0;
    private static final int MSG_DISPLAY_TOAST_INNER_BORDER = 1;
    private static final int MSG_DISPLAY_TOAST_OUTER_BORDER = 2;
    private static final int MSG_DISPLAY_TOAST_BORDER_BACK_FINISHED = 3;
    private static final int EVENT_DEFAULT = 0;
    private static final int EVENT_INNER_BORDER = 1;//最内边界
    private static final int EVENT_OUTER_BORDER = 2;//最外边界
    private static final int EVENT_BACK_FINISHED = 3;//回转到指定位置
    private static final int EVENT_NO_BORDER_FINISHED = 6;//没有边界，步数直接执行完成

    private static final int STEP_BORDER_SUBSCRIPT = 0;
    private static final int STEP_NUM_SUBSCRIPT = 1;
    /**
     * 等待马达状态超时时间
     */
    private static final int TIMEOUT = 500;

    private BorderCheckReceiver() {

    }

    public static BorderCheckReceiver getInstance() {
        synchronized (BorderCheckReceiver.class) {
            if (borderProtection == null) {
                borderProtection = new BorderCheckReceiver();
            }
        }
        return borderProtection;
    }

    /**
     * 开始监听customer-AFmoto
     */
    public void startObserving() {
        Log.d("HBK-U", "startObserving");
        borderUEventObserver.startObserving("DEVPATH=/devices/platform/customer-AFmotor");
    }

    /**
     * 结束监听customer-AFmoto
     */
    public void stopObserving() {
        Log.d("HBK-U", "stopObserving");
        //borderUEventObserver.stopObserving();
    }


    private final UEventObserver borderUEventObserver = new UEventObserver() {

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            String state = event.get("MOTOR_STATE");
            Log.d("HBK-U", "onUEvent: " + state);
            //异常超时，结束兼听，不再更改状态机状态
/*            if (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("999")) {
                return;
            }*/

            String[] borderUEvent = state.split("_");
            if (borderUEvent[STEP_BORDER_SUBSCRIPT] == null || borderUEvent[STEP_NUM_SUBSCRIPT] == null) {
                return;
            }

            int borderType = Integer.parseInt(borderUEvent[STEP_BORDER_SUBSCRIPT]);
            int borderStep = Integer.parseInt(borderUEvent[STEP_NUM_SUBSCRIPT]);
            switch (borderType) {
                case EVENT_NO_BORDER_FINISHED: {
                    mHandler.removeCallbacksAndMessages(null);

                    //SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "2");
                    MotorUtil.mMotorState = MotorUtil.MOTOR_NO_BORDER_FINISHED;
                    String[] steppingdDirection = MotorUtil.steppingdDirectionValue.split(" ");
                    if (steppingdDirection[0].equals("5")) {
                        MotorUtil.mTotalStepsBack = MotorUtil.mTotalStepsBack - borderStep;
                    } else if (steppingdDirection[0].equals("2")){
                        MotorUtil.mTotalStepsBack = MotorUtil.mTotalStepsBack + borderStep;
                    }
                    Log.d("HBK-U", "Receive EVENT_NO_BORDER_FINISHED" + " mTotalStepsBack : " + MotorUtil.mTotalStepsBack);
                    break;
                }
                case EVENT_INNER_BORDER: {
                    Log.d("HBK-U", "Receive INNER_BORDER");
                    MotorUtil.IS_TURN_ROUND = true;
                    MotorUtil.turnRoundStep = borderStep;
                    MotorUtil.TraversalGapStep = MotorUtil.DEFAULT_STEP;
                    mHandler.postDelayed(timeOutRunnable, TIMEOUT);
                    break;
                }
                case EVENT_OUTER_BORDER: {
                    Log.d("HBK-U", "Receive OUTER_BORDER");
                    MotorUtil.turnRoundStep = borderStep;
                    MotorUtil.IS_TURN_ROUND = true;
                    MotorUtil.TraversalGapStep = MotorUtil.DEFAULT_STEP;
                    mHandler.postDelayed(timeOutRunnable, TIMEOUT);
                    break;
                }
                case EVENT_BACK_FINISHED: {
                    mHandler.removeCallbacksAndMessages(null);
                    if (MotorUtil.IS_TURN_ROUND) {
                        MotorUtil.turnRoundStep -= borderStep;
                        MotorUtil.mMotorState = MotorUtil.MOTOR_BORDER_FINISHED;
                        //AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
/*                        if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
                            Log.d("HBK-U", "电机走了" + MotorUtil.turnRoundStep + "步触发限位,电机回退" + borderStep + "步，先计算拉普拉斯");
                            SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "2");
                        } else {

                            SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
                            AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
                        }*/
                        MotorUtil.IS_TURN_ROUND = false;
                    }
                }
                break;
                default: {
                    break;
                }

            }
        }
    };

    private final Runnable timeOutRunnable = () -> {
        Log.d("HBK-U", "电机回转超时，启动超时机制");
        if (MotorUtil.IS_TURN_ROUND) {
            AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
/*            if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
                Log.d("HBK-U", "触发限位大于100，先计算laps:" + MotorUtil.turnRoundStep);
                SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "2");
            } else {
                SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
                AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
            }*/
            MotorUtil.IS_TURN_ROUND = false;
        }
    };

    private final static Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();
            switch (msg.what) {
                case MSG_DISPLAY_TOAST_INNER_BORDER:
                    ToastUtil.showToast(context, R.string.toast_border_check_inner);
                    break;
                case MSG_DISPLAY_TOAST_OUTER_BORDER:
                    ToastUtil.showToast(context, R.string.toast_border_check_outer);
                    break;
                case MSG_DISPLAY_TOAST_BORDER_BACK_FINISHED:
                    ToastUtil.showToast(context, R.string.toast_border_check_back_finished);
                    break;
                default:
                    break;
            }
        }
    };
}
