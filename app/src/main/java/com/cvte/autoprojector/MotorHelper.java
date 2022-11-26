package com.cvte.autoprojector;

import static android.security.KeyStore.getApplicationContext;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UEventObserver;
import android.util.Log;

import com.cvte.autoprojector.util.AutoFocusUtil;
import com.cvte.autoprojector.util.MotorUtil;
import com.cvte.autoprojector.util.ToastUtil;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MotorHelper {
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
    private static final int STEPPING_TIMEOUT = 3000;
    private String MANUAL_MOTOR_NODE = "sys/devices/platform/customer-AFmotor/step_set";
    private int mTotalSteps = 0;
    private int mTotalStepsBack = 0;
    public final String YS_DIRECTION_STOP = "0";
    public final String YS_DIRECTION_REDUCE = "2";
    public final String YS_DIRECTION_PLUS = "5";
    public final String PLUS_VALUE_YS = "5 3000";
    public final String REDUCE_VALUE_YS = "2 3000";
    public final String PLUS_VALUE_DEFDULT = "5 3000";
    public final String REDUCE_VALUE_DEFDULT = "2 3000";
    public final String MOTOR_STOP_YS = "0 3000";

    enum MotorStatus {
        MOTOR_STATE_STOP,
        MOTOR_STATE_TURNROUND,  // 电机发生了反转
        MOTOR_STATE_STEPPING
    }

    enum MotorError {
        MOTOR_TIMEOUT_STEPPING,
        MOTOR_TIMEOUT_BORDER
    }

    private MotorStatus mMotorStatus = MotorStatus.MOTOR_STATE_STOP;
    private int mTurnRoundStep = 0;
    private static MotorHelper mMotorHelper;
    private OnMotorListener mMotorListener;
    private String sMotorDirection;
    private String steppingDirectionValue = YS_DIRECTION_STOP;
    private static final String TAG = MotorHelper.class.getName();

    private MotorHelper() {
    }

    public static MotorHelper getInstance() {
        synchronized (MotorHelper.class) {
            if (mMotorHelper == null) {
                mMotorHelper = new MotorHelper();
            }
        }
        return mMotorHelper;
    }

    public void initMotor(OnMotorListener MotorListener) {
        mMotorListener = MotorListener;
        steppingDirectionValue = MOTOR_STOP_YS;
    }

    public void setMotorSteps(String direction, int steps) {
        sMotorDirection = direction;
        steppingDirectionValue = sMotorDirection + " " + steps;
        writeSys(MANUAL_MOTOR_NODE, steppingDirectionValue);
        mMotorStatus = MotorStatus.MOTOR_STATE_STEPPING;
        if (direction.equals(YS_DIRECTION_PLUS)) {
            mTotalSteps -= steps;
        } else if (direction.equals(YS_DIRECTION_REDUCE)) {
            mTotalSteps += steps;
        } else {
            Log.d(TAG, "motor direction is abnormal " + direction);
        }
        Log.d(TAG, "steppingDirectionValue " + steppingDirectionValue);
        mHandler.postDelayed(timeOutRunnable, steps*3);
    }

    public void setMotorForewordEnd() {
        //foreword
        steppingDirectionValue = PLUS_VALUE_DEFDULT;
        String[] steppingdDirection = steppingDirectionValue.split(" ");
        sMotorDirection = steppingdDirection[0];
        writeSys(MANUAL_MOTOR_NODE, steppingDirectionValue);
        Log.d(TAG, "steppingdDirectionValue " + steppingDirectionValue +
                "sMotorDirection" + sMotorDirection);
    }

    public void setMotorBackwardEnd() {
        //backward
        steppingDirectionValue = REDUCE_VALUE_DEFDULT;
        String[] steppingdDirection = steppingDirectionValue.split(" ");
        sMotorDirection = steppingdDirection[0];
        writeSys(MANUAL_MOTOR_NODE, steppingDirectionValue);
        Log.d(TAG, "steppingdDirectionValue " + steppingDirectionValue);
    }

    /*****************************************
     * function：写文件设备
     * parameter: ①写的设备文件(IO口)，②值
     * return: 无
     *****************************************/
    private static void writeSys(String dir, String value) {
        File file = new File(dir);
        try {
            OutputStream os = new FileOutputStream(file);
            if (os != null) {
                byte[] data = value.getBytes();
                os.write(data);
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        borderUEventObserver.stopObserving();
    }

    private final UEventObserver borderUEventObserver = new UEventObserver() {

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            String state = event.get("MOTOR_STATE");
            Log.d(TAG, "onUEvent: " + state);

            String[] borderUEvent = state.split("_");
            if (borderUEvent[STEP_BORDER_SUBSCRIPT] == null || borderUEvent[STEP_NUM_SUBSCRIPT] == null) {
                return;
            }

            int borderType = Integer.parseInt(borderUEvent[STEP_BORDER_SUBSCRIPT]);
            int borderStep = Integer.parseInt(borderUEvent[STEP_NUM_SUBSCRIPT]);
            switch (borderType) {
                case EVENT_NO_BORDER_FINISHED: {
                    mHandler.removeCallbacksAndMessages(null);
                    mMotorStatus = MotorStatus.MOTOR_STATE_STOP;
                    if (sMotorDirection.equals(YS_DIRECTION_PLUS)) {
                        mTotalStepsBack -= borderStep;
                    } else if (sMotorDirection.equals(YS_DIRECTION_REDUCE)) {
                        mTotalStepsBack += borderStep;
                    }
                    mMotorListener.onMotorStepComplete(sMotorDirection, borderStep);
/*                    Log.d(TAG, "EVENT_NO_BORDER_FINISHED borderType " + borderType + " borderStep " +
                            borderStep + " mTotalStepsBack " + mTotalStepsBack);*/
                    break;
                }
                case EVENT_INNER_BORDER: {
                    mMotorStatus = MotorStatus.MOTOR_STATE_TURNROUND;
                    mTurnRoundStep = borderStep;
                    mHandler.postDelayed(timeOutRunnable, TIMEOUT);
                    break;
                }
                case EVENT_OUTER_BORDER: {
                    mTurnRoundStep = borderStep;
                    mMotorStatus = MotorStatus.MOTOR_STATE_TURNROUND;
                    mHandler.postDelayed(timeOutRunnable, TIMEOUT);
                    break;
                }
                case EVENT_BACK_FINISHED: {
                    mHandler.removeCallbacksAndMessages(null);
                    //Log.d(TAG, "EVENT_OUTER_BORDER borderType " +  borderType + " borderStep " + borderStep);
                    if (mMotorStatus == MotorStatus.MOTOR_STATE_TURNROUND) {
                        mTurnRoundStep -= borderStep;
                        mMotorListener.onMotorBorder(sMotorDirection, mTurnRoundStep);
                        mMotorStatus = MotorStatus.MOTOR_STATE_STOP;
                    }
                }
                break;
                default: {
                    break;
                }
            }
            Log.d(TAG, " borderType " +  borderType + " borderStep " + borderStep);
        }
    };

    private final Runnable timeOutRunnable = () -> {

        if (mMotorStatus == MotorStatus.MOTOR_STATE_TURNROUND) {
            Log.d(TAG, "电机回转超时，启动超时机制");
            mMotorListener.onMotorError(MotorError.MOTOR_TIMEOUT_BORDER);
        } else if (mMotorStatus == MotorStatus.MOTOR_STATE_STEPPING) {
            mMotorListener.onMotorError(MotorError.MOTOR_TIMEOUT_STEPPING);
            Log.d(TAG, "电机步进时，启动超时机制");
        }
        mMotorStatus = MotorStatus.MOTOR_STATE_STOP;
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

    public interface OnMotorListener {
        void onMotorInnerBorder(int direction, int steps);

        void onMotorOuterBorder(int direction, int steps);

        void onMotorBorder(String direction, int steps);

        void onMotorError(MotorError error);

        void onMotorStepComplete(String direction, int steps);
    }
}
