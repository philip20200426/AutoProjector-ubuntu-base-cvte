package com.cvte.autoprojector;

import static android.security.KeyStore.getApplicationContext;
import static com.cvte.autoprojector.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.autoprojector.util.FileUtil;
import com.cvte.autoprojector.util.ImageUtil;
import com.cvte.autoprojector.util.ToastUtil;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.cvte.autoprojector.util.ImageUtil;

public class AutoFocusManager {
    enum AfStateMachine {
        NOINIT,
        INIT,
        RUN,
        RUNNING,
        STOP,
        IMAGEPROCESS,
        ABNORMAL,
        EXIT
    }

    private static MotorHelper mMotorHelper;
    private static AfStateMachine mStateMachineAF = AfStateMachine.INIT;

    public static final String YS_DIRECTION_REDUCE = "2";
    public static final String YS_DIRECTION_PLUS = "5";
    public static final String YS_DIRECTION_STOP = "0";
    public static final int TOTAL_STEPS = 2340;
    public static final int INTERVAL_STEPS = 26;
    private static String mMotorDirection = YS_DIRECTION_REDUCE;
    private static int mNextSteps = 500;
    private static int mActualSteps = 0;
    private static int mReversalFlag = 0;
    private static boolean mAutoFocusRunning = true;
    private static final String TAG = AutoFocusManager.class.getName();
    private static Context context = getApplicationContext();


    public static void climbingMethodAF(ImageManager imageManager) {
        while (mAutoFocusRunning) {
            //Log.d(TAG, "mStateMachineAF " + mStateMachineAF);
            switch (mStateMachineAF) {
                case INIT:
                    initProcess();
                    mStateMachineAF = AfStateMachine.STOP;
/*                    mStateMachineAF = AfStateMachine.RUNNING;
                    mMotorHelper.setMotorForewordEnd();
                    reverseDirection();*/
                    break;
                case RUN:
                    mReversalFlag = 0;
                    mMotorHelper.setMotorSteps(mMotorDirection, mNextSteps);
                    mStateMachineAF = AfStateMachine.RUNNING;
                    break;
                case STOP:
                    imageManager.increaseImageCount();
                    SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "1");
                    mStateMachineAF = AfStateMachine.IMAGEPROCESS;
                    break;
                case IMAGEPROCESS:
                    imageProcess(imageManager);
                    //imageProcessTest(imageManager);
                    break;
                case EXIT:
/*                    FileUtil.exportCsv("/sdcard/DCIM/image.csv", imageManager);
                    ImageUtil.saveClearBitmap(imageManager);*/
                    imageManager.clear();
                    mAutoFocusRunning = false;
                    Log.d(TAG, "climbingMethodAF exit ");
                    break;
                case ABNORMAL:
                    mNextSteps = 0;
                    mMotorHelper.setMotorSteps(YS_DIRECTION_STOP, mNextSteps);
                    Log.d(TAG, "climbingMethodAF Abnormal, MOTOR STOP ");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mStateMachineAF = AfStateMachine.INIT;
                    ToastUtil.showToast(context, "Go to INIT");
                    break;
                default:
                    break;
            }
        }
    }

    public static void initProcess() {
        mNextSteps = 500;
        //mNextSteps = TOTAL_STEPS / INTERVAL_STEPS;   // test
        mMotorDirection = YS_DIRECTION_REDUCE;
        mMotorHelper = MotorHelper.getInstance();
        mMotorHelper.startObserving();
        mMotorHelper.initMotor(new MotorHelper.OnMotorListener() {
            @Override
            public void onMotorInnerBorder(int direction, int steps) {
                Log.d(TAG, " MotorInnerCallBack direction " + direction + " steps " + steps);
            }

            @Override
            public void onMotorOuterBorder(int direction, int steps) {
                Log.d(TAG, " MotorOuterCallBack direction " + direction + " steps " + steps);
            }

            @Override
            public void onMotorBorder(String direction, int steps) {
                mReversalFlag = 1;
                mMotorDirection = direction;
                mActualSteps = steps;
                mStateMachineAF = AfStateMachine.STOP;
                Log.d(TAG, " MotorCallBack Border direction " +
                        direction + " steps " + steps);
            }

            @Override
            public void onMotorError(MotorHelper.MotorError error) {
                mStateMachineAF = AfStateMachine.ABNORMAL;
                Log.d(TAG, " CALE BACK error " + error);
            }

            @Override
            public void onMotorStepComplete(String direction, int steps) {
                mMotorDirection = direction;
                mActualSteps = steps;
                mStateMachineAF = AfStateMachine.STOP;
                Log.d(TAG, " MotorCallBack StepComplete direction " + direction + " steps " + steps);
            }
        });
    }

    public static void imageProcessTest(ImageManager imageManager) {
        while (SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1")) ;
        if (imageManager.getImageSize() < INTERVAL_STEPS) {
            if (mReversalFlag == 1) {
                reverseDirection();
                mReversalFlag = 0;
            }
            mStateMachineAF = AfStateMachine.RUN;
        } else {    // 判断趋势
            mStateMachineAF = AfStateMachine.EXIT;
        }
    }


    public static void imageProcess(ImageManager imageManager) {
        //Log.d(TAG, "ImageCount "+imageManager.getImageCount());
        if (imageManager.getImageCount() == 0) {
            if (imageManager.getImageSize() < 2) {
                mStateMachineAF = AfStateMachine.RUN;
            } else {    // 判断趋势
/*                for (int j = 0; j < imageManager.getImageSize(); j++) {
                    Log.d("philip", "j "+j+" current position laplace " + imageManager.getImageList().get(j).getLaplacian() +
                            " size " + imageManager.getImageSize());
                }*/
                double currentLaplace0 = imageManager.getImageList().get(imageManager.getImageSize() - 1).getLaplacian();
                double preLapLaplace1 = imageManager.getImageList().get(imageManager.getImageSize() - 2).getLaplacian();

                if (imageManager.getImageSize() > 2) {
                    double preLapLaplace2 = imageManager.getImageList().get(imageManager.getImageSize() - 3).getLaplacian();
                    if (mReversalFlag == 1 && mActualSteps < 100 && (currentLaplace0 > preLapLaplace2)) {
                        mStateMachineAF = AfStateMachine.EXIT;
                        mReversalFlag = 0;
                        // 109CM
                        Log.d(TAG, "MOTOR BORDER : autoFocus exit, the current position is the optimal value");
                        return;
                    }
                }

                if (mReversalFlag == 1 && mActualSteps < 100) {
                    reverseDirection();
                    mReversalFlag = 0;
                    mStateMachineAF = AfStateMachine.RUN;
                    Log.d(TAG, "MOTOR BORDER : Only reverse the direction, no longer calculate the trend");
                    return;
                }
                if (currentLaplace0 > preLapLaplace1) { //forward
                    mStateMachineAF = AfStateMachine.RUN;
                    if (mReversalFlag == 1) {
                        mStateMachineAF = AfStateMachine.EXIT;
                        mReversalFlag = 0;
                    }
                } else {
                    mStateMachineAF = AfStateMachine.RUN;
                    reverseDirection();
                    if (mNextSteps < 80) {
                        Log.d(TAG, "autoFocus exit, mNextSteps " + mNextSteps);
                        mStateMachineAF = AfStateMachine.EXIT;
                        mMotorHelper.setMotorSteps(mMotorDirection, mNextSteps);
                    }
                    mNextSteps = (int) (mNextSteps * 0.382);
                    if (mReversalFlag == 1) {
                        mReversalFlag = 0;
                        mNextSteps = mActualSteps / 2;
                        Log.d(TAG, "MOTOR BORDER : autoFocus exit, mNextSteps " + mNextSteps);
                    }
                }
            }
        }
    }

    public static void reverseDirection() {
        if (mMotorDirection.equals(YS_DIRECTION_REDUCE)) {
            mMotorDirection = YS_DIRECTION_PLUS;
        } else {
            mMotorDirection = YS_DIRECTION_REDUCE;
        }
        Log.d(TAG, "mMotorDirection " + mMotorDirection);
    }


}
