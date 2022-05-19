package com.cvte.camera2demo.util;

import android.os.Handler;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

public class AutoFocusUtil {

    public static final int AUTO_FOCUS_FINISHED_TO_EXIT = 0;
    public static final int AUTO_FOCUS_NULL = 1;
    public static final int AUTO_FOCUS_INCREASE = 2;
    public static final int AUTO_FOCUS_TURN_ROUND = 3;
    public static final int AUTO_FOCUS_TO_CLEAREST = 4;
    public static int autoFocusState = AUTO_FOCUS_INCREASE;

    public static void setAutoFocusOrigin() {
        // 1. 将马达转到初始位置
        Log.d("HBK","将马达转到初始位置");
        //backward
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        MotorUtil.setMotorForeword();
        ImageUtil.cleanLaplaceValue();
        try {
            Thread.sleep(MotorUtil.routeTotalTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stop
        MotorUtil.setMotorStop();
    }

    public static void setAutoFocusTraversal() {
        // 2. 马达转动整个过程拍摄所有画面
        Log.d("HBK", "马达转动整个过程拍摄所有画面");
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        MotorUtil.setMotorBackward();
        try {
            Thread.sleep(MotorUtil.routeTotalTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stop
        Log.d("HBK","拍摄过程stop");
        MotorUtil.setMotorStop();
        ImageUtil.laplaceMaxCount = ImageUtil.laplaceCounter;
        Log.d("HBK"," maxCount :" + ImageUtil.laplaceMaxCount);
    }

    public static void calculateAutoFocusLaplaceMax() {
        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
        Log.d("HBK", "计算最大清晰位置");
        for (int i = 0; i < ImageUtil.laplaceMaxCount; i++) {
            if (ImageUtil.laplaceBiggestValue < ImageUtil.laplaceValue[i]) {
                ImageUtil.laplaceBiggestValue = ImageUtil.laplaceValue[i];
                ImageUtil.laplaceBiggestCount = i;
                Log.d("HBK", "MAX value:" + ImageUtil.laplaceBiggestValue + " count:" + ImageUtil.laplaceBiggestCount);
            }
        }
        Log.d("HBK", "Adjust back millisecond : " + (MotorUtil.routeTotalTime - ImageUtil.laplaceBiggestCount * MotorUtil.routeTotalTime / ImageUtil.laplaceMaxCount));
    }

    public static void setAutoFocusToPosition() {
        Log.d("HBK", "按比例回转到对应的位置");
        MotorUtil.setMotorForeword();
        // 回调n秒
        // handlerAdjust.postDelayed(new closeHandler(), (ImageUtil.laplaceBiggestCount / maxCount * 2500));
        try {
            int time = MotorUtil.routeTotalTime - ImageUtil.laplaceBiggestCount * MotorUtil.routeTotalTime / ImageUtil.laplaceMaxCount - 275;
            if (time < 0) {
                time = 0;
            }
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stop
        MotorUtil.setMotorStop();

        //finished and clean cache data
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        ImageUtil.cleanLaplaceValue();
        SystemPropertiesAdapter.set("persist.cvte.AUTO_PROJECTOR_ALLOW","1");
    }

    public static void saveAutoFocusFinishedToKeystone() {
        ImageUtil.AutoFocusFinishedToKeystone = true;
    }

    public static void setAutoFocusGapTraversal() {
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        Log.d("HBK-GAP","GAP-拍摄过程start");
        MotorUtil.setMotorRun();
        try {
            Thread.sleep(MotorUtil.TraversalGapTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MotorUtil.setMotorStop();
        Log.d("HBK-GAP","GAP-拍摄过程stop");
        ImageUtil.laplaceMaxCount = ImageUtil.laplaceCounter;
        Log.d("HBK-GAP","ImageUtil.laplaceMaxCount = " + ImageUtil.laplaceMaxCount);
        //计算并判断此次采集的数据是否递增，设置状态机下一个状态
        calculateAutoFocusLaplaceGapMax();

        Log.d("HBK-GAP","ImageUtil.laplaceBiggestCount = " + ImageUtil.laplaceBiggestCount);
        Log.d("HBK-GAP","ImageUtil.laplaceMaxCount = " + ImageUtil.laplaceMaxCount);

        if(ImageUtil.laplaceBiggestCount == ImageUtil.laplaceMaxCount - 1) {
            autoFocusState = AUTO_FOCUS_INCREASE;
        } else if (ImageUtil.laplaceBiggestCount == 0) {
            autoFocusState = AUTO_FOCUS_TURN_ROUND;
        } else {
            autoFocusState = AUTO_FOCUS_TO_CLEAREST;
        }
        Log.d("HBK-GAP","autoFocusState = " + autoFocusState);
    }

    public static void calculateAutoFocusLaplaceGapMax() {
        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
        Log.d("HBK-GAP", "计算最大清晰位置");
        for (int i = 0; i < ImageUtil.laplaceMaxCount; i++) {
            if (ImageUtil.laplaceBiggestValue < ImageUtil.laplaceValue[i]) {
                ImageUtil.laplaceBiggestValue = ImageUtil.laplaceValue[i];
                ImageUtil.laplaceBiggestCount = i;
                Log.d("HBK-GAP", "MAX value:" + ImageUtil.laplaceBiggestValue + " count:" + ImageUtil.laplaceBiggestCount);
            }
        }
        Log.d("HBK-GAP", "Adjust back millisecond : " + (MotorUtil.TraversalGapTime - ImageUtil.laplaceBiggestCount * MotorUtil.TraversalGapTime / ImageUtil.laplaceMaxCount));
    }

    public static void setAutoFocusToGapPosition() {
        Log.d("HBK", "按比例回转到对应的位置");
        MotorUtil.setMotorForeword();
        // 回调n秒
        // handlerAdjust.postDelayed(new closeHandler(), (ImageUtil.laplaceBiggestCount / maxCount * 2500));
        try {
            int time = MotorUtil.TraversalGapTime - ImageUtil.laplaceBiggestCount * MotorUtil.TraversalGapTime / ImageUtil.laplaceMaxCount;
            if (time < 0) {
                time = 0;
            }
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stop
        MotorUtil.setMotorStop();

        //finished and clean cache data
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        ImageUtil.cleanLaplaceValue();
        SystemPropertiesAdapter.set("persist.cvte.AUTO_PROJECTOR_ALLOW","1");
    }
}
