package com.cvte.autoprojector.util;

import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MotorUtil {
    public static final String MANUAL_FOCUS_IO_FOREWORD = "/sys/class/gpio/gpio39/value";
    public static final String MANUAL_FOCUS_IO_BACKWARD = "/sys/class/gpio/gpio40/value";
    public static final String MANUAL_FOCUS_IO_FOREWORD_ON = "1";
    public static final String MANUAL_FOCUS_IO_FOREWORD_OFF = "0";
    public static final String MANUAL_FOCUS_IO_BACKWARD_ON = "1";
    public static final String MANUAL_FOCUS_IO_BACKWARD_OFF = "0";

    public static final String MANUAL_MOTOR_NODE_DEF = "/sys/class/pwm_in/pwm-in/pwm_in";
    public static final String PLUS_VALUE_DEF = "SetSteptoMotor:direction=1,pwm_num=64,extend=2,";
    public static final String REDUCE_VALUE_DEF = "SetSteptoMotor:direction=0,pwm_num=64,extend=2,";
    public static final String MOTOR_STOP_DEF = "SetSteptoMotor:direction=0,pwm_num=64,extend=0,";
    public static final String MANUAL_MOTOR_NODE_YS = "sys/devices/platform/customer-AFmotor/step_set";
    public static final String PLUS_VALUE_YS = "5 3000";
    public static final String REDUCE_VALUE_YS = "2 3000";
    public static String PLUS_VALUE_DEFDULT = "5 3000";
    public static String REDUCE_VALUE_DEFDULT = "2 3000";

    public static final String MOTOR_STOP_YS = "0 3000";
    public static final String PLUS_VALUE_YS_NO_STEP = "5 ";
    public static final String REDUCE_VALUE_YS_NO_STEP = "2 ";
    public static final String MOTOR_STOP_YS_NO_STEP = "0 ";
    public static final String YS_DIRECTION_PLUS = "5";
    public static final String YS_DIRECTION_REDUCE = "2";
    public static final String YS_DIRECTION_STOP = "0";
    public static final String MANUAL_MOTOR_NODE_RGL_885 = "/sys/devices/platform/yn_steppermotor/ctrl_start";
    public static final String PLUS_VALUE_RGL_885 = "0 180";
    public static final String REDUCE_VALUE_RGL_885 = "1 180";
    public static final String MOTOR_STOP_RGL_885 = "2 0";

    public static String MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_DEF;
    public static String PLUS_VALUE = PLUS_VALUE_DEF;
    public static String REDUCE_VALUE = REDUCE_VALUE_DEF;

    public static String MOTOR_STOP = MOTOR_STOP_DEF;
    public static final Boolean CVT_EN_REMOTE_CONTROL_FOCUS = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_REMOTE_CONTROL_FOCUS", false);
    public static final Boolean CVT_EN_YISHU_FOCUS = SystemPropertiesAdapter.getBoolean("persist.sys.auto_foucs", false);
    public static final int CVT_DEF_STEP_MOTOR_TYPE = SystemPropertiesAdapter.getInt("ro.CVT_DEF_STEP_MOTOR_TYPE", 1);
    public static final int MOTOR_DC_WANBO = 0;
    public static final int MOTOR_STEP_WANBO = 1;
    public static final int MOTOR_STEP_YS = 2;
    public static final int MOTOR_STEP_RGL_885 = 3;
    public static String steppingdDirectionValue = MOTOR_STOP;
    public static String DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_OFF;
    public static String DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;

    public static int TraversalGapTime = 1000;//ms
    public static int routeTotalTime = 0;//ms
    public static int mCurrentSteps = 0;

    public static final int MOTOR_STATE_NONE = -1;
    public static final int MOTOR_BORDER_FINISHED = 1;
    public static final int MOTOR_NO_BORDER_FINISHED = 2;
    public static int mMotorState = MOTOR_STATE_NONE;
    public static int mTotalSteps = 0;
    public static int mTotalStepsBack = 0;
    /**
     * 电机默认步数
     */
    public static final int DEFAULT_STEP = 1200;
    /**
     * 有效步数
     */
    public static final int EFFECTIVE_STEPS = 100;

    public static int TraversalGapStep = DEFAULT_STEP;
    /**
     * 电机发生了反转
     */
    public static boolean IS_TURN_ROUND = false;
    /**
     * 驱动返回的电机步数
     */
    public static int turnRoundStep = 0;

    public MotorUtil() {
        initStepMotorStatus();
    }

    public static void initStepMotorStatus() {
        Log.d("HBK-Y", "initStepMotorStatus");
        int motorType = SystemPropertiesAdapter.getInt("ro.CVT_DEF_STEP_MOTOR_TYPE", 2);
        Log.d("HBK-Y", "motorType:" + motorType);
        switch (motorType) {
            case MotorUtil.MOTOR_DC_WANBO: {
                if (CVT_EN_YISHU_FOCUS) {
                    Log.d("HBK-Y", "MOTOR_DC_YISHU");
                    routeTotalTime = 2167;//ms
                } else {
                    routeTotalTime = 2400;//ms
                }
            }
            break;
            case MotorUtil.MOTOR_STEP_WANBO: {
                MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_DEF;
                PLUS_VALUE = PLUS_VALUE_DEF;
                REDUCE_VALUE = REDUCE_VALUE_DEF;
                MOTOR_STOP = MOTOR_STOP_DEF;
                steppingdDirectionValue = MOTOR_STOP;
                routeTotalTime = 3700;//ms
                TraversalGapTime = 1000;//ms
            }
            break;
            case MotorUtil.MOTOR_STEP_YS: {
                MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_YS;
                PLUS_VALUE = PLUS_VALUE_YS;
                REDUCE_VALUE = REDUCE_VALUE_YS;
                MOTOR_STOP = MOTOR_STOP_YS;
                steppingdDirectionValue = MOTOR_STOP;
                routeTotalTime = 2167;//ms
                TraversalGapTime = 500;//ms
            }
            break;
            case MotorUtil.MOTOR_STEP_RGL_885: {
                MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_RGL_885;
                PLUS_VALUE = PLUS_VALUE_RGL_885;
                REDUCE_VALUE = REDUCE_VALUE_RGL_885;
                MOTOR_STOP = MOTOR_STOP_RGL_885;
                steppingdDirectionValue = MOTOR_STOP;
                //临时用于第一套遍历，和第二套逐次逼近
                routeTotalTime = 3500;//ms
                TraversalGapTime = 1500;//ms
            }
            break;
            default: {
                if (CVT_EN_REMOTE_CONTROL_FOCUS) {
                    MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_DEF;
                    PLUS_VALUE = PLUS_VALUE_DEF;
                    REDUCE_VALUE = REDUCE_VALUE_DEF;
                    MOTOR_STOP = MOTOR_STOP_DEF;
                    steppingdDirectionValue = MOTOR_STOP;
                    routeTotalTime = 3700;//ms
                    TraversalGapTime = 1000;//ms
                } else if (CVT_EN_YISHU_FOCUS) {
                    Log.d("HBK-Y", "initStepMotorStatus 一数");
                    MANUAL_MOTOR_NODE = MANUAL_MOTOR_NODE_YS;
                    PLUS_VALUE = PLUS_VALUE_YS;
                    REDUCE_VALUE = REDUCE_VALUE_YS;
                    MOTOR_STOP = MOTOR_STOP_YS;
                    steppingdDirectionValue = MOTOR_STOP;
                    routeTotalTime = 2167;//ms
                    TraversalGapTime = 500;//ms
                    Log.d("HBK-Y", "MOTOR_STEP_YISHU");
                    Log.d("HBK-Y", "MANUAL_MOTOR_NODE = " + MANUAL_MOTOR_NODE);
                    Log.d("HBK-Y", "PLUS_VALUE = " + PLUS_VALUE);
                    Log.d("HBK-Y", "REDUCE_VALUE = " + REDUCE_VALUE);
                    Log.d("HBK-Y", "MOTOR_STOP = " + MOTOR_STOP);
                    Log.d("HBK-Y", "routeTotalTime = " + routeTotalTime);
                    Log.d("HBK-Y", "TraversalGapTime = " + TraversalGapTime);
                }
            }
            break;
        }
        Log.d("HBK-Y", " routeTotalTime: " + routeTotalTime);
    }

    public static boolean isStepMotor() {
        boolean ret = false;
        if (CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)
                || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_YS) || CVT_EN_YISHU_FOCUS
                || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_RGL_885)) {
            ret = true;
        }
        return ret;
    }
    public static void setMotorForeword() {
        //foreword
        Log.d("HBK-885", "Foreword isStepMotor(): " + isStepMotor() + " " + MANUAL_MOTOR_NODE + " " + PLUS_VALUE);
        if (isStepMotor()) {
            writeSys(MANUAL_MOTOR_NODE, PLUS_VALUE);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_ON);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);
        }
    }
    public static void setMotorForewordEnd() {
        //foreword
        Log.d("HBK-885", "philip Foreword isStepMotor(): " + isStepMotor() + " " + MANUAL_MOTOR_NODE + " " + PLUS_VALUE);
        if (isStepMotor()) {
            steppingdDirectionValue = PLUS_VALUE_DEFDULT;
            writeSys(MANUAL_MOTOR_NODE, PLUS_VALUE_DEFDULT);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_ON);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);
        }
        Log.d("HBK-BC", "philip steppingdDirectionValue = " + steppingdDirectionValue);
    }

    public static void setMotorBackward() {
        //backward
        Log.d("HBK-885", "Backward isStepMotor():" + isStepMotor());
        if (isStepMotor()) {
            Log.d("HBK-885", "philip setMotorBackward " + MANUAL_MOTOR_NODE+"  "+REDUCE_VALUE);
            writeSys(MANUAL_MOTOR_NODE, REDUCE_VALUE);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_ON);
        }
    }
    public static void setMotorBackwardEnd() {
        //backward
        Log.d("HBK-885", "Backward isStepMotor():" + isStepMotor());
        if (isStepMotor()) {
            steppingdDirectionValue = REDUCE_VALUE_DEFDULT;
            writeSys(MANUAL_MOTOR_NODE, REDUCE_VALUE_DEFDULT);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_ON);
        }
        Log.d("HBK-BC", "philip steppingdDirectionValue = " + steppingdDirectionValue);
    }
    public static void setMotorStop() {
        //stop
        Log.d("HBK-885", "setMotorStop:" + MANUAL_MOTOR_NODE +  MOTOR_STOP);
        if (isStepMotor()) {
            writeSys(MANUAL_MOTOR_NODE, MOTOR_STOP);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);
        }
    }


    public static void setMotorTurnRound() {
        if (isStepMotor()) {
            setSteppingMotorTurnRound();
        } else {
            setDCMotorTurnRound();
        }
    }

    public static void setDCMotorTurnRound() {
        if (DCDirectionValueIOF.equals(MANUAL_FOCUS_IO_FOREWORD_ON)) {
            DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_OFF;
            DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_ON;
        } else {
            DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_ON;
            DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;
        }
    }

    public static void setSteppingMotorTurnRound() {
        Log.d("philip", "setSteppingMotorTurnRound >>>>>>" + steppingdDirectionValue +"  PLUS_VALUE "+PLUS_VALUE);
        if (steppingdDirectionValue.equals(PLUS_VALUE)) {
            steppingdDirectionValue = REDUCE_VALUE;
        } else {
            steppingdDirectionValue = PLUS_VALUE;
        }
        Log.d("philip", "setSteppingMotorTurnRound >>>>>>" + steppingdDirectionValue);
    }

    public static void setMotorReversal() {
        String[] steppingdDirection = steppingdDirectionValue.split(" ");
        if (steppingdDirection[0].equals(YS_DIRECTION_PLUS)) {
            steppingdDirectionValue = "2 ";
        } else {
            steppingdDirectionValue = "5 ";
        }
        Log.d("philip", "setSteppingMotorTurnRound >>>>>>" + steppingdDirectionValue +
                "pri direction : " + steppingdDirection[0]);
    }

    public static void setMotorRun() {
        //foreword
        Log.d("HBK-885", "isStepMotor():" + isStepMotor());
        if (isStepMotor()) {
            writeSys(MANUAL_MOTOR_NODE, steppingdDirectionValue);
            Log.d("HBK-885", "writeSys:" + MANUAL_MOTOR_NODE + " " + steppingdDirectionValue);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, DCDirectionValueIOF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, DCDirectionValueIOB);
        }
    }

    public static void setMotorIOStartStatus() {
        steppingdDirectionValue = PLUS_VALUE;
        DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_ON;
        DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;
        Log.d("HBK-885", "PLUS_VALUE" + steppingdDirectionValue);
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
    private static int getMotorSteps() {
        return mCurrentSteps;
    }
    /*****************************************
     * function：步进电机转到相应的步数
     * parameter: ①步数
     * return: 无
     *****************************************/
    public static void setMotorRunInOrderStep(int step) {
        String[] steppingdDirection = steppingdDirectionValue.split(" ");
        if (steppingdDirection[0] == null || steppingdDirection[1] == null) {
            return;
        }
        mCurrentSteps = step;
        Log.d("HBK-BC", "steppingdDirection[0] = " + steppingdDirection[0] + ",steppingdDirection[1] = " + steppingdDirection[1]);
        if (steppingdDirection[0].equals(YS_DIRECTION_PLUS)) {
            steppingdDirectionValue = PLUS_VALUE_YS_NO_STEP + step;
            PLUS_VALUE = steppingdDirectionValue;
            writeSys(MANUAL_MOTOR_NODE, steppingdDirectionValue);
            mTotalSteps = mTotalSteps - step;
        } else if (steppingdDirection[0].equals(YS_DIRECTION_REDUCE)) {
            steppingdDirectionValue = REDUCE_VALUE_YS_NO_STEP + step;
            REDUCE_VALUE = steppingdDirectionValue;

            writeSys(MANUAL_MOTOR_NODE, steppingdDirectionValue);
            mTotalSteps = mTotalSteps + step;
        } else {
            Log.d("HBK-BC", "steppingdDirectionValue = null");
        }
        Log.d("HBK-BC", "philip steppingdDirectionValue : " + steppingdDirectionValue +
                " mTotalSteps : " + mTotalSteps);
    }

    public static void resetStep() {
        TraversalGapStep = 1000;
    }
}
