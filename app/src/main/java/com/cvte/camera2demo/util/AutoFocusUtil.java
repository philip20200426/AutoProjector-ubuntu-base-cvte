package com.cvte.camera2demo.util;

import static com.cvte.camera2demo.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;

import android.os.Handler;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.camera2demo.ImageManager;

import org.opencv.core.Mat;

public class AutoFocusUtil {
    /**
     * 对焦完成退出
     */
    public static final int AUTO_FOCUS_FINISHED_TO_EXIT = 0;
    public static final int AUTO_FOCUS_NULL = 1;
    /**
     * 上升趋势
     */
    public static final int AUTO_FOCUS_INCREASE = 2;

    public static final int AUTO_FOCUS_DECLINE = 8;
    /**
     * 下降趋势
     */
    public static final int AUTO_FOCUS_TURN_ROUND = 3;
    /**
     * 出现波峰
     */
    public static final int AUTO_FOCUS_TO_CLEAREST = 4;

    public static final int AUTO_FOCUS_TO_CLEAREST_CHECK = 5;
    public static final int AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF = 6;
    public static final int AUTO_FOCUS_WAIT_FOR_CALCULATE_CLEAREST = 7;
    public static int autoFocusState = AUTO_FOCUS_INCREASE;
    public static int autoFocusStatePrevious;
    //全向自动校正开关
    public static final String AUTO_ALL_CORRECTION = "persist.cvte.auto_all_correction";

    public static void setAutoFocusOrigin() {
        // 1. 将马达转到初始位置
        Log.d("HBK", "将马达转到初始位置");
        //backward
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        ImageUtil.AutoFocusFinishedToKeystone = false;
        ImageUtil.KeystonePositiveFinishedToNegative = false;
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
        ImageUtil.AutoFocusFinishedToKeystone = false;
        ImageUtil.KeystonePositiveFinishedToNegative = false;
        MotorUtil.setMotorBackward();
        try {
            Thread.sleep(MotorUtil.routeTotalTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stop
        Log.d("HBK", "拍摄过程stop");
        MotorUtil.setMotorStop();
        ImageUtil.laplaceMaxCount = ImageUtil.laplaceCounter;
        Log.d("HBK", " maxCount :" + ImageUtil.laplaceMaxCount);
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
            int time = MotorUtil.routeTotalTime - ImageUtil.laplaceBiggestCount * MotorUtil.routeTotalTime / ImageUtil.laplaceMaxCount;
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
        SystemPropertiesAdapter.set("persist.cvte.AUTO_PROJECTOR_ALLOW", "1");
    }

    public static void saveAutoFocusFinishedToKeystone() {
        ImageUtil.AutoFocusFinishedToKeystone = true;
    }

    public static void setAutoFocusGapTraversal() {
        ImageUtil.laplaceCounter = 0;
        ImageUtil.laplaceBiggestValue = 0;
        ImageUtil.laplaceBiggestCount = 0;
        Log.d("HBK-GAP", "GAP-拍摄过程start");
        MotorUtil.setMotorRun();
        try {
            Thread.sleep(MotorUtil.TraversalGapTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MotorUtil.setMotorStop();
        Log.d("HBK-GAP", "GAP-拍摄过程stop");
        ImageUtil.laplaceMaxCount = ImageUtil.laplaceCounter;
        Log.d("HBK-GAP", "[Traversal] laplaceMaxCount = " + ImageUtil.laplaceMaxCount);
        //计算并判断此次采集的数据是否递增，设置状态机下一个状态
        calculateAutoFocusLaplaceGapMax();
//        //计算方差
//        calculateAutoFocusLaplaceGapStandardDeviation();
        Log.d("HBK-GAP", "[MAX]laplaceBiggestCount = " + ImageUtil.laplaceBiggestCount);
        Log.d("HBK-GAP", "[MAX]laplaceMaxCount = " + ImageUtil.laplaceMaxCount);


        if (autoFocusState == AUTO_FOCUS_TO_CLEAREST_CHECK) {
            if ((Math.abs(ImageUtil.laplaceMinimumCount - ImageUtil.laplaceMaxCount / 2) < ImageUtil.laplaceMaxCount / 3) //如果最小值在中间，则表示还没找到最清晰的值
                    || (Math.abs(ImageUtil.laplaceBiggestCount - ImageUtil.laplaceMinimumCount) < 3)                 //如果最大值最小值相邻，则表示还没找到最清晰的值
                    || (Math.abs(ImageUtil.laplace2thBiggestCount - ImageUtil.laplaceMinimumCount) < 3)              //如果最大值最小值相邻，则表示还没找到最清晰的值
            ) {
                Log.d("HBK-GAP", "[END END] 最小值在中间1/3区间 或者 最大值最小值相邻，还没找到最清晰的值");
                autoFocusState = AUTO_FOCUS_TURN_ROUND;
            } else {
                autoFocusState = AUTO_FOCUS_TO_CLEAREST;
            }
        } else {
            if (ImageUtil.laplaceMaxCount - ImageUtil.laplaceBiggestCount == 1) { //最后几个数有概率波动不处理
                autoFocusState = AUTO_FOCUS_INCREASE;
            } else if (ImageUtil.laplaceBiggestCount == 0) {//前几个数有概率波动不处理
                autoFocusState = AUTO_FOCUS_TURN_ROUND;
            } else {
                if ((Math.abs(ImageUtil.laplace2thBiggestCount - ImageUtil.laplaceBiggestCount) > 3)                       //如果最大值和第二大值相差很远，说明数据有明显抖动
                        || (Math.abs(ImageUtil.laplaceBiggestCount - ImageUtil.laplaceMinimumCount) < 3)                 //如果最大值和最小值相邻，则表示还没找到最清晰的值
                        || (Math.abs(ImageUtil.laplace2thBiggestCount - ImageUtil.laplaceMinimumCount) < 3)              //如果第二大值和最小值相邻，则表示还没找到最清晰的值
                        || (Math.abs(ImageUtil.laplaceMinimumCount - ImageUtil.laplaceMaxCount / 2) < ImageUtil.laplaceMaxCount / 3) //如果最小值在中间，则表示还没找到最清晰的值
                ) {
                    autoFocusState = AUTO_FOCUS_TURN_ROUND;
                } else {
                    autoFocusState = AUTO_FOCUS_TO_CLEAREST_CHECK;
                }
            }
        }

        //清除标准差计算过程值
        ImageUtil.laplaceGapStandardDeviation = 0;
        ImageUtil.laplaceGapValueSum = 0;
        Log.d("HBK-GAP", "autoFocusState = " + autoFocusState);
    }

    public static void calculateAutoFocusLaplaceGapMax() {
        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
        Log.d("HBK-GAP", "计算最大清晰位置");
        ImageUtil.laplaceMinimumValue = ImageUtil.laplaceValue[0];
        for (int i = 0; i < ImageUtil.laplaceMaxCount; i++) {
            //计算总和，方便后续计算方差
            ImageUtil.laplaceGapValueSum = ImageUtil.laplaceGapValueSum + ImageUtil.laplaceValue[i];

            //找出最大值
            if (ImageUtil.laplaceBiggestValue < ImageUtil.laplaceValue[i]) {
                ImageUtil.laplace2thBiggestValue = ImageUtil.laplaceBiggestValue;
                ImageUtil.laplace2thBiggestCount = ImageUtil.laplaceBiggestCount;
                ImageUtil.laplaceBiggestValue = ImageUtil.laplaceValue[i];
                ImageUtil.laplaceBiggestCount = i;
            }
            //找出最小值
            if (ImageUtil.laplaceMinimumValue > ImageUtil.laplaceValue[i]) {
                ImageUtil.laplaceMinimumValue = ImageUtil.laplaceValue[i];
                ImageUtil.laplaceMinimumCount = i;
            }
        }
        Log.d("HBK-GAP", "[GapMax] MAX value:" + ImageUtil.laplaceBiggestValue + " count:" + ImageUtil.laplaceBiggestCount);
        Log.d("HBK-GAP", "[GapMax] 2th MAX value:" + ImageUtil.laplace2thBiggestValue + " 2th count:" + ImageUtil.laplace2thBiggestCount);
        Log.d("HBK-GAP", "[GapMax] MIN value:" + ImageUtil.laplaceMinimumValue + " count:" + ImageUtil.laplaceMinimumCount);
        Log.d("HBK-GAP", "[GapMax] Adjust back millisecond : " + (MotorUtil.TraversalGapTime - ImageUtil.laplaceBiggestCount * MotorUtil.TraversalGapTime / ImageUtil.laplaceMaxCount));
    }

    //方差s^2=[(x1-x)^2 +...(xn-x)^2]/n 或者s^2=[(x1-x)^2 +...(xn-x)^2]/(n-1)
    //标准差σ=sqrt(s^2)
    public static void calculateAutoFocusLaplaceGapStandardDeviation() {
        double laplaceGapVariance = 0;
        double laplaceGapValueAverage = 0;
        laplaceGapValueAverage = ImageUtil.laplaceGapValueSum / ImageUtil.laplaceMaxCount;
        for (int i = 0; i < ImageUtil.laplaceMaxCount; i++) {//求方差
            laplaceGapVariance += (ImageUtil.laplaceValue[i] - laplaceGapValueAverage) * (ImageUtil.laplaceValue[i] - laplaceGapValueAverage);
        }

        ImageUtil.laplaceGapStandardDeviation = Math.sqrt(laplaceGapVariance / ImageUtil.laplaceMaxCount);
        Log.d("HBK-GAP", "Variance: " + laplaceGapVariance + ",StandardDeviation: " + ImageUtil.laplaceGapStandardDeviation);
    }

    public static void setAutoFocusToGapPosition() {
        Log.d("HBK", "按比例回转到对应的位置");
        //判断是否为误判
        MotorUtil.setMotorRun();
        Log.d("HBK-GAP", "[END END] 上一次最大计数" + ImageUtil.laplaceMaxCountCheck);
        Log.d("HBK-GAP", "[END END] 上一次最大值下标" + ImageUtil.laplaceBiggestCountCheck);
        Log.d("HBK-GAP", "[END END] 本次最大计数" + ImageUtil.laplaceMaxCount);
        Log.d("HBK-GAP", "[END END] 本次最大值下标" + ImageUtil.laplaceBiggestCount);
        Log.d("HBK-GAP", "CheckNum: " + (ImageUtil.laplaceMaxCountCheck - ImageUtil.laplaceBiggestCountCheck) + ",BiggestCount: " + ImageUtil.laplaceBiggestCount);

        // 回调n秒
        // handlerAdjust.postDelayed(new closeHandler(), (ImageUtil.laplaceBiggestCount / maxCount * 2500));
        try {
            //Check过程中计算的回调量
            int timeCheck = ImageUtil.laplaceBiggestCountCheck * MotorUtil.TraversalGapTime / ImageUtil.laplaceMaxCountCheck;
            Log.d("HBK-GAP", "[END END] Check过程中计算的回调量" + timeCheck);
            if (timeCheck > MotorUtil.TraversalGapTime) {
                timeCheck = MotorUtil.TraversalGapTime;
            }

            //确认过check OK之后，回到clearest过程的回调量
            int time = MotorUtil.TraversalGapTime - ImageUtil.laplaceBiggestCount * MotorUtil.TraversalGapTime / ImageUtil.laplaceMaxCount;
            Log.d("HBK-GAP", "[END END] clearest过程的回调量" + time);
            if (time < 0) {
                time = 0;
            }
            int timeAvg = (time + timeCheck) / 2;
            Log.d("HBK-GAP", "[END END] timeAvg过程的回调量" + timeAvg);
            Thread.sleep(timeAvg);
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
        SystemPropertiesAdapter.set("persist.cvte.AUTO_PROJECTOR_ALLOW", "1");
        //设置状态机结束，退出
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
    }


    public static void setAutoFocusTraversalByStep(int step) {
        //如果发生重新流程，触发限位记录应清空
        MotorUtil.turnRoundStep = 0;
        //跑500步 同时拍照，并返回拍摄的图片集
        Log.d("HBK-BC", "resetBitmapPool");
        //1.复位拍照数据
        ImageUtil.resetBitmapPool();
        Log.d("HBK-BC", "PERSIST_BEGIN_TAKE_PHOTO=1");
        //2.开始拍照
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
        Log.d("HBK-BC", "setMotorRunInOrderStep");
        //3.开始跑500步
        MotorUtil.setMotorRunInOrderStep(step);
        //4.跑完500步，在BorderCheck中收到结束UEvent结束拍照
    }

    /**
     * 对焦找到最清晰的点，根据下标回到相应位置
     * step=MotorUtil.TraversalGapStep-ImageUtil.bitmapPoolBiggestIndex * MotorUtil.TraversalGapStep / ImageUtil.bitmapPoolLength
     * 目前存在问题：
     * 1.发生反转回到清晰的图片位置不对
     * 2.回到最清晰位置触发了限位
     * 3.发生反转，电机反转步数太大，需要和驱动确认，目前添加了经验值发生反转：马达反转步数+清晰图（步数）（待测试）
     */
    public static void setAutoFocusToBitmapPoolPosition() {
        Log.d("HBK-BC", "ToBitmapPoolPosition");
        //确认过check OK之后，回到clearest过程的回调量
        int step = MotorUtil.TraversalGapStep - ImageUtil.bitmapPoolBiggestIndex * MotorUtil.TraversalGapStep / ImageUtil.bitmapPoolLength;
        Log.d("HBK-GAP", "[END] 最清晰图片回调量" + step);
        //电机状态 3时候可能会走了异常步数,
        if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
            int totalStep = MotorUtil.turnRoundStep;
            step = totalStep - ImageUtil.bitmapPoolBiggestIndex * totalStep / ImageUtil.bitmapPoolLength;
            Log.d("HBK-GAP", "[END] 发生反转重新计算-总行程：" + totalStep + "返回清晰图片回调量：" + step);
        }
        //不能出现负数
        step = Math.max(step, 0);
        MotorUtil.setMotorRunInOrderStep(step);
    }

    public static void setAutoFocusToBitmapPoolPosition(ImageManager imageManager) {
        Log.d("HBK-BC", "ToBitmapPoolPosition");

        int step = imageManager.getReturnSteps(MotorUtil.TraversalGapStep);
        Log.d("HBK-GAP", "[END] 最清晰图片回调量:" + step);
//        if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
//            step += 200;
//        }
        //不能出现负数
        step = Math.max(step, 0);
        MotorUtil.setMotorRunInOrderStep(step);
    }
}
