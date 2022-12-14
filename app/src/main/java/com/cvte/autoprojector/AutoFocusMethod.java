package com.cvte.autoprojector;

import static com.cvte.autoprojector.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;
import static com.cvte.autoprojector.util.Constants.PERSIST_FINISH_TAKE_PHOTO;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.util.ImageUtils;
import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.autoprojector.util.AutoFocusUtil;
import com.cvte.autoprojector.util.ImageUtil;
import com.cvte.autoprojector.util.MotorUtil;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.max;
import static org.opencv.core.Core.mean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoFocusMethod {
    private static ExecutorService executor;
    private boolean isFirstAutoFocus = false;
    private MotorHelper motorHelper;
    /**
     * 开始计算的第一张图片索引，正常情况0，
     * 因为现在前三张图片概率性出现计算清晰度异常
     */
    private static final int FIRST_BITMAP_INDEX = 3;
    private static List<LocationBean> locationList = new ArrayList<>();

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

    private static long beginTime;


    /**
     * 查找清晰度最大的值
     */
    public static int calculateLocationPoolLaplaceMax() {
        if (locationList.isEmpty()) {
            return -1;
        }
        LocationBean maxLapsLocation = locationList.get(0);
        for (int i = 0; i < locationList.size(); i++) {
            LocationBean locationBean = locationList.get(i);
            if (locationBean != null) {
                if (locationBean.getLaplacian() > maxLapsLocation.getLaplacian()) {
                    maxLapsLocation = locationBean;
                }
            }
        }
        Log.d("philip", "LocationPool MAX value:" + maxLapsLocation.getLaplacian() + " index:" + maxLapsLocation.getIndex());
        return maxLapsLocation.getIndex();
    }

    public static void dataCaptureAF(ImageManager imageManager) {
        Log.d("philip", "autoFocusTest start," + " autoFocusState: " + AutoFocusUtil.autoFocusState);
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
        MotorUtil.setMotorForewordEnd();

        long now = SystemClock.uptimeMillis();
        int num = 26;
        int nextSumSteps = 2340;
        int intervalSteps = 0;
        int motorStopMs = 200;
        int multiImageMs = 1;
        int stepsThreshold = 120;
        int i = 0;
        intervalSteps = nextSumSteps / num;
        imageManager.setmLocation(num + 1);
        SystemPropertiesAdapter.set(PERSIST_FINISH_TAKE_PHOTO, "0");
        Log.d("philip", " motorStopMs : " + motorStopMs + " multiImageMs : " + multiImageMs);
        Log.d("philip", " sumSteps : " + nextSumSteps + " intervalSteps : " + intervalSteps + "  START START START");
        while (i < num + 1) {
            if (MotorUtil.mMotorState == MotorUtil.MOTOR_BORDER_FINISHED) {
                Log.d("philip", "i : " + i + "  MOTOR_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  !!!!!!!!!!");
                MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
                try {
                    Thread.sleep(motorStopMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;

                if (i == 0) {
                    //MotorUtil.setMotorReversal();
                    MotorUtil.setMotorTurnRound();
                    MotorUtil.setMotorRunInOrderStep(intervalSteps);
                } else {
                    Log.d("philip", " MOTOR_BORDER_FINISHED , i !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!   " + i);
                }
                i++;
            }
            if (MotorUtil.mMotorState == MotorUtil.MOTOR_NO_BORDER_FINISHED) {
                MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
                Log.d("philip", "i : " + i + "  MOTOR_NO_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  >>>>>>>>>>");
                try {
                    Thread.sleep(motorStopMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;

                if (i < num) {
                    MotorUtil.setMotorRunInOrderStep(intervalSteps);
                }
                i++;
            }
        }
    }

    public static void autoFocusLoop(ImageManager imageManager) {
        Log.d("philip", "autoFocusTest start," + " autoFocusState: " + AutoFocusUtil.autoFocusState);
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
        MotorUtil.setMotorForewordEnd();

        long now = SystemClock.uptimeMillis();
        int num = 5;
        int nextSumSteps = 2300;
        int intervalSteps = 0;
        int motorStopMs = 1;
        int multiImageMs = 1;
        int stepsThreshold = 120;
        int i = 0;
        Log.d("philip", " motorStopMs : " + motorStopMs + " multiImageMs : " + multiImageMs);
        while (true) {
            intervalSteps = nextSumSteps / num;
            imageManager.setmLocation(num + 1);
            SystemPropertiesAdapter.set(PERSIST_FINISH_TAKE_PHOTO, "0");
/*            if (nextSumSteps < 400) {
                multiImageMs = 300;
            }*/
            Log.d("philip", " sumSteps : " + nextSumSteps + " intervalSteps : " + intervalSteps + "  START START START");
            while (i < num + 1) {
                if (MotorUtil.mMotorState == MotorUtil.MOTOR_BORDER_FINISHED) {
                    Log.d("philip", "i : " + i + "  MOTOR_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  !!!!!!!!!!");
                    MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
                    try {
                        Thread.sleep(motorStopMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                    while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;

                    if (i == 0) {
                        //MotorUtil.setMotorReversal();
                        MotorUtil.setMotorTurnRound();
                        MotorUtil.setMotorRunInOrderStep(intervalSteps);
                    } else {
                        Log.d("philip", " MOTOR_BORDER_FINISHED , i !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!   " + i);
                    }
                    i++;
                }
                if (MotorUtil.mMotorState == MotorUtil.MOTOR_NO_BORDER_FINISHED) {
                    MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
                    Log.d("philip", "i : " + i + "  MOTOR_NO_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  >>>>>>>>>>");
                    try {
                        Thread.sleep(motorStopMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                    while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;

                    if (i < num) {
                        MotorUtil.setMotorRunInOrderStep(intervalSteps);
                    }
                    i++;
                }
            }
            Log.d("philip", "-------------------------------------- E");
            while (SystemPropertiesAdapter.get(PERSIST_FINISH_TAKE_PHOTO, "0").equals("0")) ;
            Log.d("philip", "-------------------------------------- X");

            if (i > 0) {

                nextSumSteps = intervalSteps * 2;
                //int maxIndex = calculateLocationPoolLaplaceMax();
                int maxIndex = imageManager.getMaxLapsIndex();
                imageManager.clear();

                MotorUtil.setMotorTurnRound();
                int nextSteps = 0;
                int nextIndex = maxIndex + 1;
                int xishu = 1;
                if (maxIndex < num) {
                    if (nextSumSteps < stepsThreshold) {
                        nextIndex = maxIndex;
                    }
                } else {
                    nextIndex = maxIndex;
                    xishu = 0;
                }
                //nextSteps = (num-nextIndex)*intervalSteps + xishu*intervalSteps/2;
                nextSteps = (num - nextIndex) * intervalSteps;
                MotorUtil.setMotorRunInOrderStep(nextSteps);
                Log.d("philip", "Number: " + num + " maxIndex: " + maxIndex +
                        " Go to index: " + nextIndex + " nextSteps: " + nextSteps);
                if (nextSumSteps < stepsThreshold) {
                    Log.d("philip", "Autofocus Exit END END END");
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
/*                num = num -2;
                if (num < 3) {
                    num = 3;
                }*/
                i = 0;
            }
        }
    }

    public static void fineSearchAF(ImageManager imageManager) {
        Log.d("philip", "autoFocusTest start," + " autoFocusState: " + AutoFocusUtil.autoFocusState);
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
        //MotorUtil.setMotorForewordEnd();
        MotorUtil.setMotorTurnRound();
        MotorUtil.setMotorRunInOrderStep(1);

        long now = SystemClock.uptimeMillis();
        int num = 1;
        int nextSumSteps = 500;
        int intervalSteps = 0;
        int motorStopMs = 1;
        int multiImageMs = 1;
        int stepsThreshold = 50;
        int i = 0;
        Log.d("philip", " motorStopMs : " + motorStopMs + " multiImageMs : " + multiImageMs);
        while (true) {
            intervalSteps = nextSumSteps / num;
            imageManager.setmLocation(num + 1);
            SystemPropertiesAdapter.set(PERSIST_FINISH_TAKE_PHOTO, "0");

            Log.d("philip", " sumSteps : " + nextSumSteps + " intervalSteps : " + intervalSteps + "  START START START");
            while (i < num + 1) {
                if (MotorUtil.mMotorState == MotorUtil.MOTOR_BORDER_FINISHED) {
                    Log.d("philip", "i : " + i + "  MOTOR_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  !!!!!!!!!!");
                    MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
/*                    try {
                        Thread.sleep(motorStopMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                    while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;


                    if (i == 0) {
                        //MotorUtil.setMotorReversal();
                        MotorUtil.setMotorTurnRound();
                        MotorUtil.setMotorRunInOrderStep(intervalSteps);
                    } else {
                        Log.d("philip", " MOTOR_BORDER_FINISHED , i !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!   " + i);
                    }
                    i++;
                }
                if (MotorUtil.mMotorState == MotorUtil.MOTOR_NO_BORDER_FINISHED) {
                    MotorUtil.mMotorState = MotorUtil.MOTOR_STATE_NONE;
                    Log.d("philip", "i : " + i + "  MOTOR_NO_BORDER_FINISHED , intervalSteps : " + intervalSteps + "  >>>>>>>>>>");
/*                    try {
                        Thread.sleep(motorStopMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                    while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;

                    if (i < num) {
                        MotorUtil.setMotorRunInOrderStep(intervalSteps);
                    }
                    i++;
                }
            }
            Log.d("philip", "-------------------------------------- E");
            while (SystemPropertiesAdapter.get(PERSIST_FINISH_TAKE_PHOTO, "0").equals("0")) ;
            Log.d("philip", "-------------------------------------- X");

            if (i > 0) {

                nextSumSteps = intervalSteps * 2;
                //int maxIndex = calculateLocationPoolLaplaceMax();
                int maxIndex = imageManager.getMaxLapsIndex();
                imageManager.clear();
                int nextSteps = 0;
                if (i == 2) {
                    switch (maxIndex) {
                        case 0://back
                            MotorUtil.setMotorTurnRound();
                            nextSumSteps = intervalSteps / 2;
                            Log.d("roise", "move back " + " nextSumSteps " + nextSumSteps);
/*                            if (nextSumSteps < stepsThreshold) {
                                nextSteps = intervalSteps;
                            }*/
                            break;
                        case 1: //forward
                            nextSumSteps = intervalSteps;
                            Log.d("roise", "move forward " + " nextSumSteps " + nextSumSteps);
                            break;
                        default:
                            break;
                    }
                }

                i = 0;
                if (nextSumSteps < stepsThreshold) {
                    MotorUtil.setMotorRunInOrderStep(intervalSteps);
                    Log.d("philip", "Autofocus Exit END END END, nextSumSteps : " + nextSumSteps +
                            " TotalSteps : " + MotorUtil.mTotalSteps);
                    break;
                } else {
                    MotorUtil.setMotorRunInOrderStep(nextSteps);
                }
            }
        }
    }

    /*****************************************
     * function：AutoFocusMethod③ 纯步数和限位UEvent，逐次逼近算法
     * @return null
     *****************************************/
    public static void autoFocusStepBorderCheckFunc(ImageManager imageManager) {
        beginTime = SystemClock.uptimeMillis();
        // 1. 设置状态机初始状态
        Log.d("HBK-BC", "autoFocusStepBorderCheckFunc--------------start----------");
        MotorUtil.setMotorIOStartStatus();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        executor = Executors.newFixedThreadPool(10);
        // 2. 进入逐次逼近状态机，找到最佳位置
        Log.d("HBK-BC", "Begin,autoFocusState = " + AutoFocusUtil.autoFocusState);
        while (AutoFocusUtil.autoFocusState != AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT) {
            switch (AutoFocusUtil.autoFocusState) {
                case AutoFocusUtil.AUTO_FOCUS_INCREASE: {
                    //Step 1:跑500步，拍照，计算拉普拉斯值
                    Log.d("HBK-BC", "BC-【1】INCREASE");
                    //跑500步 同时拍照，并返回拍摄的图片集
                    imageManager.clear();
                    AutoFocusUtil.setAutoFocusTraversalByStep(MotorUtil.TraversalGapStep);
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF;
                    Log.d("HBK-BC", "BC-【1】INCREASE END");
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_TURN_ROUND: {
                    Log.d("HBK-BC", "BC-【2】TURN_ROUND");
                    //设置方向反向
                    MotorUtil.setMotorTurnRound();
                    //发生回转，往回走的时候多走 1/3
//                    int optimalNumberOfSteps = getOptimalNumberOfSteps();
//                    Log.d("HBK-BC", "optimalNumberOfSteps: " + optimalNumberOfSteps);
                    imageManager.clear();
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
//                    AutoFocusUtil.setAutoFocusToBitmapPoolPosition();
                    AutoFocusUtil.setAutoFocusToBitmapPoolPosition(imageManager);
                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_CLEAREST;
                    //保存清晰图片
//                    saveClearBitmap();
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_DEF: {
                    Log.d("HBK-BC", "BC-【T-1】WAIT_FOR_CALCULATE_DEF");
                    //等待拍照拍完上抛UEvent改变状态
                    while (finishedTakePhoto()) {
                        //计算这500步数据池图片的拉普拉斯值，并算出最大值
//                        calculateDataPoolPhotoClarity();
                        imageManager.calculateDataPoolPhotoClarity();
                        //切换状态机器
//                        AutoFocusUtil.autoFocusState = getBitmapPoolState();
                        AutoFocusUtil.autoFocusState = getBitmapPoolState(imageManager);
                        //拍完的照片计算完毕，复位拍照状态
                        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
                    }
                    break;
                }
                case AutoFocusUtil.AUTO_FOCUS_WAIT_FOR_CALCULATE_CLEAREST: {
                    Log.d("HBK-BC", "BC-【T-2】WAIT_FOR_CALCULATE_CLEAREST");
                    while (finishedTakePhoto()) {
                        //finished and clean cache data
                        ImageUtil.resetBitmapPool();
                        //清空
                        imageManager.clear();
                        //切换状态机器->设置状态机结束，退出
                        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
                        //拍完的照片计算完毕，复位拍照状态
                        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
                    }
                    break;
                }
                default: {
                    ImageUtil.lastBitmapPoolBiggestValue = 0.0;
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
        Log.d("HBK-BC", "autoFocusStepBorderCheckFunc--------------end----------耗时" + (SystemClock.uptimeMillis() - beginTime));
    }

    /**
     * 设置最佳步数，优化对焦速度
     * 1.数据呈上升趋势，步数1000
     * 2.数据呈下降趋势第一个值为最大值步数
     * 确保能再次拍到之前丢弃掉的图片
     */
    private static int getOptimalNumberOfSteps() {
        if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
            MotorUtil.TraversalGapStep = MotorUtil.turnRoundStep;
        } else {
            MotorUtil.TraversalGapStep = MotorUtil.DEFAULT_STEP;
        }
//        float scaleStep = (float) biggestIndex / (float) bitmapLength;
        float scaleStep = 1 / 3f;//发生反转，多走1/3
        Log.d("HBK-BC", "scaleStep: " + scaleStep);
        MotorUtil.TraversalGapStep += MotorUtil.TraversalGapStep * scaleStep;
        return Math.max(MotorUtil.TraversalGapStep, MotorUtil.DEFAULT_STEP);
    }

    /*****************************************
     please add sub function under this line
     *****************************************/
    private static boolean finishedTakePhoto() {
        return SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("2");
    }

    /**
     * 计算数据池图片清晰度
     */
    public static void calculateDataPoolPhotoClarity() {
        Log.d("HBK-BC-L", "Laplace start--------------------------------");
        long timeBegin = SystemClock.uptimeMillis();
        //计算拍下来图片的清晰度的值
        for (int i = 0; i < ImageUtil.bitmapPoolLength; i++) {
//            ImageUtil.laplaceValue[i] = calculateOneBitmapClarity(ImageUtil.bitmapPool[i]);
            ImageUtil.laplaceValue[i] = calculateOneBitmapClarityWithNoGray(ImageUtil.bitmapPool[i]);
            Log.d("HBK-BC-L", "laplaceValue[" + i + "]:" + ImageUtil.laplaceValue[i]);
        }
        //找出最大值下标
        calculateBitmapPoolLaplaceMax();
        Log.d("HBK-BC-L", "Laplace end--------------------------耗时：" + (SystemClock.uptimeMillis() - timeBegin));
    }

    /**
     * 查找清晰度最大的值
     */
    public static void calculateBitmapPoolLaplaceMax() {
        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
        Log.d("HBK-BC-L", "find the max value in the taken photo bitmapPoolLength:" + ImageUtil.bitmapPoolLength);
        for (int i = 0; i < ImageUtil.bitmapPoolLength; i++) {
            if (ImageUtil.bitmapPoolBiggestValue < ImageUtil.laplaceValue[i]) {
                ImageUtil.bitmapPoolBiggestValue = ImageUtil.laplaceValue[i];
                ImageUtil.bitmapPoolBiggestIndex = i;
            }
        }
        Log.d("HBK-BC-L", "上次清晰度: " + ImageUtil.lastBitmapPoolBiggestValue);
        if (ImageUtil.bitmapPoolBiggestValue > ImageUtil.lastBitmapPoolBiggestValue) {
            ImageUtil.lastBitmapPoolBiggestValue = ImageUtil.bitmapPoolBiggestValue;
        }
        Log.d("HBK-BC-L", "本次清晰度: " + ImageUtil.bitmapPoolBiggestValue);
        Log.d("HBK-BC-L", "Pool MAX value:" + ImageUtil.bitmapPoolBiggestValue + " index:" + ImageUtil.bitmapPoolBiggestIndex);
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

    /**
     * 保存出现最清晰图片
     *
     * @return
     */
    public static void saveClearBitmap() {
        String prefix = "Laplacian";
        ImageUtil.removeLocalImages(prefix);
        for (int i = 0; i < ImageUtil.bitmapPoolLength; i++) {
            Bitmap srcBitmap = ImageUtil.bitmapPool[i];
            double lapValue = ImageUtil.laplaceValue[i];
            Mat mat = new Mat();
            Utils.bitmapToMat(srcBitmap, mat);
            mat = cutImgROI(mat);
            Mat finalMat = mat;
            int finalIndex = i;
            executor.execute(() -> {

                Log.d("HBK-BC", "保存测试图片------------------start");
                String fileName = "/sdcard/DCIM/Laplacian[" + finalIndex + "]-" + lapValue + ".png";
                Imgcodecs.imwrite(fileName, finalMat);
                Log.d("HBK-BC", "保存测试图片--------------------end");
            });
        }
    }

    /**
     * 计算一个没有灰色的位图清晰度
     *
     * @param srcBitmap 图片
     * @return
     */
    public static double calculateOneBitmapClarityWithNoGray(Bitmap srcBitmap) {
        int kernel_size = 3;
        int ddepth = CvType.CV_16U;
        Mat mat = new Mat();
        Utils.bitmapToMat(srcBitmap, mat);
        mat = cutImgROI(mat);
        Mat resizeMat = new Mat();
        Imgproc.resize(mat, resizeMat, new org.opencv.core.Size(128, 128));
        Mat lapMat = new Mat();
        Imgproc.Laplacian(resizeMat, lapMat, ddepth, kernel_size);
        Scalar mean = mean(lapMat);
        return mean.val[0];
    }

    /**
     * 切图，生成新的 Mat
     *
     * @param bitmap 原始图片Bitmap
     * @return Mat
     */
    public static Mat cutImgROI(Mat bitmap) {
        int startRow = 132, endRow = 132 + 256;
        int startCol = 512, endCol = 512 + 256;
        Range areaRow = new Range(startRow, endRow);
        Range areaCol = new Range(startCol, endCol);
        return new Mat(bitmap, areaRow, areaCol);
    }

    /***
     * 根据图片池改变状态
     * @return 返回当前状态
     */
    public static int getBitmapPoolState() {
        int ret;
        //拍照总数
        if (ImageUtil.bitmapPoolBiggestIndex == ImageUtil.bitmapPoolLength - 1) {
            Log.d("HBK-BC", "上升趋势，当前方向继续走");
            //1.拉普拉斯值成，触发限位，发动机走的实际步数>100有效，认为找到最清晰值。
            if (MotorUtil.turnRoundStep > MotorUtil.EFFECTIVE_STEPS) {
                Log.d("HBK-BC", "上升趋势，触发限位，发动机走的实际步数>100有效，认为找到最清晰值");
                ret = AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST;
            } else {
                //2.拉普拉斯值成上升趋势，继续走1000步
                Log.d("HBK-BC", "上升趋势，触发限位，发动机走的实际步数<100有效，拉普拉斯无效继续查找");
                ret = AutoFocusUtil.AUTO_FOCUS_INCREASE;
            }
//            ret = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        } else if (ImageUtil.bitmapPoolBiggestIndex < 4) {//开始前3几张不稳定，index
            Log.d("HBK-BC", "下降趋势，前三张不稳定需要发动反转继续向前");
            //2.拉普拉斯值成上升趋势，继续走1000步
            ret = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
        } else {
            //3.出现波峰
            ret = AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST;
        }
        Log.d("HBK-BC", "bitmapPoolCounter:" + ImageUtil.bitmapPoolLength + ",bitmapPoolBiggestCounter:" + ImageUtil.bitmapPoolBiggestIndex);
        Log.d("HBK-BC", "getBitmapPoolState:" + ret);
        return ret;
    }

    /***
     * 根据图片池改变状态
     * @return 返回当前状态
     */
    public static int getBitmapPoolState(ImageManager imageManager) {
        int ret;
        //拍照总数
        if (imageManager.getMaxLapsIndex() == imageManager.getImageList().size() - 1) {
            Log.d("HBK-BC", "上升趋势，当前方向继续走");
            //1.拉普拉斯值成，触发限位，发动机走的实际步数>100有效，认为找到最清晰值。
            ret = AutoFocusUtil.AUTO_FOCUS_INCREASE;
        } else if (imageManager.getMaxLapsIndex() <= FIRST_BITMAP_INDEX) {//开始前FIRST_BITMAP_INDEX几张不稳定，index
            Log.d("HBK-BC", "下降趋势，前" + FIRST_BITMAP_INDEX + "张不稳定需要发动反转继续向前");
            //2.拉普拉斯值成上升趋势，继续走1000步
            ret = AutoFocusUtil.AUTO_FOCUS_TURN_ROUND;
        } else {
            //3.出现波峰
            ret = AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST;
        }
        Log.d("HBK-BC", "bitmapPoolCounter:" + imageManager.getImageSize() + ",bitmapPoolBiggestCounter:" + imageManager.getMaxLapsIndex());
        Log.d("HBK-BC", "getBitmapPoolState:" + ret);
        return ret;
    }
}
