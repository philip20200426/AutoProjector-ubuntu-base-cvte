package com.cvte.camera2demo;

import android.graphics.Bitmap;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.camera2demo.util.AutoFocusUtil;
import com.cvte.camera2demo.util.ImageUtil;
import com.cvte.camera2demo.util.MotorUtil;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.mean;

public class AutoFocusMethod {

    /*****************************************
     * function：AutoFocusMethod② 纯图像，逐次逼近算法
     * @return void
     *****************************************/
    public static void autoFocusStepSuccessiveApproximation() {
        // 1. 设置状态机初始状态
        Log.d("HBK-GAP", "GAP-设置状态机初始状态");
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        MotorUtil.setMotorIOStartStatus();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 2. 进入逐次逼近状态机，找到最佳位置
        while (AutoFocusUtil.autoFocusState != AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT) {
            switch (AutoFocusUtil.autoFocusState) {
                case AutoFocusUtil.AUTO_FOCUS_INCREASE: {
                    //跑1s，拍照，计算拉普拉斯值
                    Log.d("HBK-GAP", "GAP-【1】递增");
                    AutoFocusUtil.setAutoFocusGapTraversal();
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_TURN_ROUND: {
                    Log.d("HBK-GAP", "GAP-【2】回转");
                    //设置方向反向
                    MotorUtil.setMotorTurnRound();
                    //跑1s，拍照，计算拉普拉斯值
                    AutoFocusUtil.setAutoFocusGapTraversal();
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST_CHECK: {
                    Log.d("HBK-GAP", "GAP-【3】找到最清晰检查");
                    ImageUtil.laplaceBiggestValueCheck = ImageUtil.laplaceBiggestValue;
                    ImageUtil.laplaceBiggestCountCheck = ImageUtil.laplaceBiggestCount;
                    ImageUtil.laplaceMaxCountCheck = ImageUtil.laplaceMaxCount;
                    //设置方向反向
                    MotorUtil.setMotorTurnRound();
                    //跑1s，拍照，计算拉普拉斯值
                    AutoFocusUtil.setAutoFocusGapTraversal();
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST: {
                    Log.d("HBK-GAP", "GAP-【4】最清晰位置确认");
                    //设置方向反向(回转)
                    MotorUtil.setMotorTurnRound();
                    //算出最大值的位置，回到最大值，并设置状态机
                    AutoFocusUtil.setAutoFocusToGapPosition();
                    break;
                }
                default: {
                    //无状态，退出
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
                    break;
                }
            }
        }
        // 3.清除状态机位置
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_NULL;
    }

    /*****************************************
     * function：AutoFocusMethod③ 纯步数和限位UEvent，逐次逼近算法
     * @return null
     *****************************************/
    public static void autoFocusStepBorderCheckFunc() {
        // 1. 设置状态机初始状态
        Log.d("HBK-BC", "autoFocusStepBorderCheckFunc()");
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        MotorUtil.setMotorIOStartStatus();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. 进入逐次逼近状态机，找到最佳位置
        Log.d("HBK-BC", "Begin,autoFocusState = " + AutoFocusUtil.autoFocusState);
        while (AutoFocusUtil.autoFocusState != AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT) {
            switch (AutoFocusUtil.autoFocusState) {
                case AutoFocusUtil.AUTO_FOCUS_INCREASE: {
                    //Step 1:跑500步，拍照，计算拉普拉斯值
                    Log.d("HBK-BC", "BC-【1】INCREASE");
                    //跑500步 同时拍照，并返回拍摄的图片集
                    AutoFocusUtil.setAutoFocusTraversalByStep(MotorUtil.TraversalGapStep);
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF;
                    Log.d("HBK-BC", "BC-【1】INCREASE END");
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_TURN_ROUND: {
                    Log.d("HBK-BC", "BC-【2】TURN_ROUND");
                    //设置方向反向
                    MotorUtil.setMotorTurnRound();
                    //跑500步 同时拍照，并返回拍摄的图片集
                    AutoFocusUtil.setAutoFocusTraversalByStep(MotorUtil.TraversalGapStep);
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF;
                    break;
                }
//                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST_CHECK: {
//                    Log.d("HBK-BC", "BC-【3】CLEAREST_CHECK");
//                    ImageUtil.bitmapPoolBiggestValueCheck = ImageUtil.bitmapPoolBiggestValue;
//                    ImageUtil.bitmapPoolBiggestCountCheck = ImageUtil.bitmapPoolBiggestCounter;
//                    ImageUtil.bitmapPoolMaxCountCheck = ImageUtil.bitmapPoolCounter;
//                    //设置方向反向
//                    MotorUtil.setMotorTurnRound();
//                    //跑500步 同时拍照，并返回拍摄的图片集
//                    AutoFocusUtil.setAutoFocusTraversalByStep(MotorUtil.TraversalGapStep);
//                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF;
//                    break;
//                }
                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST: {
                    Log.d("HBK-BC", "BC-【4】TO_CLEAREST");
                    //设置方向反向(回转)
                    MotorUtil.setMotorTurnRound();
                    //算出最大值的位置，回到最大值，并设置状态机
                    AutoFocusUtil.setAutoFocusToBitmapPoolPosition();
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_CLEAREST;
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF: {
                    Log.d("HBK-BC", "BC-【T-1】WAIT_FOR_CALCULATE_DEF");
                    //等待拍照拍完上抛UEvent改变状态
                    while(finishedTakePhoto()) {
                        //计算这500步数据池图片的拉普拉斯值，并算出最大值
                        calculateDataPoolPhotoClarity();
                        //切换状态机器
                        AutoFocusUtil.autoFocusState = getBitmapPoolState();
                        //拍完的照片计算完毕，复位拍照状态
                        SystemPropertiesAdapter.set("persist.begin.take.photo","0");
                    }
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_CLEAREST: {
                    Log.d("HBK-BC", "BC-【T-2】WAIT_FOR_CALCULATE_CLEAREST");
                    while(finishedTakePhoto()) {
                        //finished and clean cache data
                        ImageUtil.resetBitmapPool();
                        //切换状态机器->设置状态机结束，退出
                        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
                        //拍完的照片计算完毕，复位拍照状态
                        SystemPropertiesAdapter.set("persist.begin.take.photo","0");
                    }
                    break;
                }
                default: {
                    //无状态，退出
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
                    break;
                }
            }
            Thread.yield();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 3.清除状态机位置
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_NULL;
    }


    /*****************************************
    please add sub function under this line
     *****************************************/
    private static boolean finishedTakePhoto() {
        boolean ret = SystemPropertiesAdapter.get("persist.begin.take.photo", "0").equals("2");
        return ret;
    }

    public static void calculateDataPoolPhotoClarity(){
        //计算拍下来图片的清晰度的值
        for(int i=0; i < ImageUtil.bitmapPoolCounter; i++){
            ImageUtil.laplaceValue[i] = calculateOneBitmapClarity(ImageUtil.bitmapPool[i]);
            Log.d("HBK-BC", "laplaceValue[" + i + "]:" + ImageUtil.laplaceValue[i]);
        }
        //找出最大值下标
        calculateBitmapPoolLaplaceMax();
    }

    public static void calculateBitmapPoolLaplaceMax() {
        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
        Log.d("HBK-BC", "find the max value in the taken photo");
        for (int i = 3; i < ImageUtil.bitmapPoolCounter; i++) {
            if (ImageUtil.bitmapPoolBiggestValue < ImageUtil.laplaceValue[i]) {
                ImageUtil.bitmapPoolBiggestValue = ImageUtil.laplaceValue[i];
                ImageUtil.bitmapPoolBiggestCounter = i;
            }
        }
        Log.d("HBK-BC", "Pool MAX value:" + ImageUtil.bitmapPoolBiggestValue + " count:" + ImageUtil.bitmapPoolBiggestCounter);
    }

    public static double calculateOneBitmapClarity(Bitmap srcBitmap) {
        int kernel_size = 3;
        int ddepth = CvType.CV_16U;
        Mat mat = new Mat();
        Utils.bitmapToMat(srcBitmap, mat);
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);
        Mat resizeMat = new Mat();
        Imgproc.resize(grayMat, resizeMat, new org.opencv.core.Size(512, 512));
        Mat lapMat = new Mat();
        Imgproc.Laplacian(resizeMat, lapMat, ddepth, kernel_size);
        Scalar mean = mean(lapMat);
        return mean.val[0];
    }

    public static int getBitmapPoolState(){
        int ret = 0;

        if(ImageUtil.bitmapPoolCounter - ImageUtil.bitmapPoolBiggestCounter == 1) {//最后算的时候加了1，所以这里计算是减完与1比较
            ret = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        } else if(ImageUtil.bitmapPoolBiggestCounter <= 3){//开始前几张不稳定，不作数
            ret = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
        } else {
            ret = AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST;
        }
        Log.d("HBK-BC", "bitmapPoolCounter:" + ImageUtil.bitmapPoolCounter + ",bitmapPoolBiggestCounter" + ImageUtil.bitmapPoolBiggestCounter);
        Log.d("HBK-BC", "getBitmapPoolState:" + ret);

        return ret;
    }

}
