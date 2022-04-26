package com.cvte.camera2demo.util;

import android.os.Handler;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

public class AutoFocusUtil {

    public static void setAutoFocusOrigin() {
        // 1. 将马达转到初始位置
        Log.d("HBK","将马达转到初始位置");
        //backward
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        Handler handlerBegin = new Handler();
        MotorUtil.setMotorForeword();
//        handlerBegin.postDelayed(new closeHandler(), 2600); // 延迟3秒，closeHandler()
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
        Handler handlerAdjust = new Handler();
        //foreword
        MotorUtil.setMotorForeword();
//        handlerAdjust.postDelayed(new closeHandler(), 2500); // 调整整个过程2.5秒，closeHandler()
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
        //backward
        Handler handlerBackAdj = new Handler();
        MotorUtil.setMotorBackward();
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

}
