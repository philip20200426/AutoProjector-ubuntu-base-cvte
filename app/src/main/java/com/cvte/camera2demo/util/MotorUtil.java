package com.cvte.camera2demo.util;

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

    public static final String MANUAL_MOTOR_NODE = "sys/devices/platform/customer-AFmotor/step_set";//"/sys/class/pwm_in/pwm-in/pwm_in";
    public static final String PLUS_VALUE = "3 3000";//"SetSteptoMotor:direction=1,pwm_num=64,extend=2,";
    public static final String REDUCE_VALUE = "6 3000";//"SetSteptoMotor:direction=0,pwm_num=64,extend=2,";
    public static final String MOTOR_STOP = "0 3000";//"SetSteptoMotor:direction=0,pwm_num=64,extend=0,";
    public static final Boolean CVT_EN_REMOTE_CONTROL_FOCUS = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_REMOTE_CONTROL_FOCUS", false);
    public static final int CVT_DEF_STEP_MOTOR_TYPE = SystemPropertiesAdapter.getInt("ro.CVT_DEF_STEP_MOTOR_TYPE", 1);
    public static final int MOTOR_DC_WANBO = 0;
    public static final int MOTOR_STEP_WANBO = 1;
    public static String steppingdDirectionValue = MOTOR_STOP;
    public static String DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_OFF;
    public static String DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;

    public static int TraversalGapTime = 1000;
    public static int routeTotalTime = 0;//ms
    public static void setMotorForeword(){
        //foreword
        if(CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)){
            writeSys(MANUAL_MOTOR_NODE, PLUS_VALUE);
            routeTotalTime = 3700;//7000;//10500;
        }else{
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_ON);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);
            routeTotalTime = 2400;
        }
    }

    public static void setMotorBackward() {
        //backward
        if (CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)) {
            writeSys(MANUAL_MOTOR_NODE, REDUCE_VALUE);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_ON);
        }
    }

    public static void setMotorStop() {
        //stop
        if (CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)) {
            writeSys(MANUAL_MOTOR_NODE, MOTOR_STOP);
        } else {
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);
        }
    }


    public static void setMotorTurnRound() {
        if (CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)) {
            setSteppingMotorTurnRound();
        } else {
            setDCMotorTurnRound();
        }
    }

    public static void setDCMotorTurnRound(){
        if (DCDirectionValueIOF.equals(MANUAL_FOCUS_IO_FOREWORD_ON)) {
            DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_OFF;
            DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_ON;
        } else {
            DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_ON;
            DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;
        }
    }

    public static void setSteppingMotorTurnRound(){
        if (steppingdDirectionValue.equals(PLUS_VALUE)) {
            steppingdDirectionValue = REDUCE_VALUE;
        } else {
            steppingdDirectionValue = PLUS_VALUE;
        }
    }

    public static void setMotorRun(){
        //foreword
        if(CVT_EN_REMOTE_CONTROL_FOCUS || (MotorUtil.CVT_DEF_STEP_MOTOR_TYPE == MotorUtil.MOTOR_STEP_WANBO)){
            writeSys(MANUAL_MOTOR_NODE, steppingdDirectionValue);
        }else{
            writeSys(MANUAL_FOCUS_IO_FOREWORD, DCDirectionValueIOF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, DCDirectionValueIOB);
        }
    }

    public static void setMotorIOStartStatus(){
        steppingdDirectionValue = PLUS_VALUE;
        DCDirectionValueIOF = MANUAL_FOCUS_IO_FOREWORD_ON;
        DCDirectionValueIOB = MANUAL_FOCUS_IO_BACKWARD_OFF;
    }

    /*****************************************
     * function：写文件设备
     * parameter: ①写的设备文件(IO口)，②值
     * return: 无
     *****************************************/
    private static void writeSys(String dir,String value){
        File file = new File(dir);
        try{
            OutputStream os = new FileOutputStream(file);
            if(os!=null){
                byte[] data = value.getBytes();
                os.write(data);
                os.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
