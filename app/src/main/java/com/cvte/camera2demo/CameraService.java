package com.cvte.camera2demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.camera2demo.util.AutoFocusUtil;
import com.cvte.camera2demo.util.ImageUtil;
import com.cvte.camera2demo.util.LogUtil;
import com.cvte.camera2demo.util.MotorUtil;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static org.opencv.core.Core.mean;

public class CameraService extends Service {
    private Camera2Helper mCamera2Helper;
    private ShowPattern mShowPattern;
    private long mLastTime = -1L;

    private Context mContext;
    Handler mHandler = new Handler();
    private int mTakePicCount = 0;
    private final int LIMIT_TAKE_PIC = 1;
    private boolean isStart = false;
    private Runnable mOverTimeRunnable;
    public static final Boolean CVT_EN_AUTO_FOCUS_APPROACH = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_AUTO_FOCUS_APPROACH", false);

    public CameraService() {
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String CHANNEL_ONE_ID = getPackageName();
            String CHANNEL_ONE_NAME = "CHANNEL_ONE_NAME";
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID, CHANNEL_ONE_NAME, IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.setShowBadge(false);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ONE_ID).build();
            startForeground(1, notification);
        }
    }

    private void stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("HBK", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (!OpenCVLoader.initDebug()) {
            Log.d("HBK", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("HBK", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void openCamera() {
        if (mCamera2Helper == null) {
            mCamera2Helper = new Camera2Helper(new Camera2Helper.OnCameraListener() {
                @Override
                public void onCameraStarted() {
                    removeLocalImgs();
                }


                @Override
                public void onCameraError(int error) {
                    mShowPattern.removeAllView();
                }

                @Override
                public void onCaptureComplete(Bitmap bitmap, File file) {
                    long now = SystemClock.uptimeMillis();
                    if (mLastTime != -1) {
                        LogUtil.d(bitmap.toString() + " get bitmap duration = " + (now - mLastTime));
                    }
                    mLastTime = now;
                    LogUtil.d("time   11");
                    // 计算拉普拉斯清晰度
                    toClarityByOpenCV(bitmap);
                    // 保存到本地
                    if (ImageUtil.AutoFocusFinishedToKeystone) {
                        String name = "n0" + (mTakePicCount) + ".png";
                        if (mTakePicCount != 0) {
                            ImageUtil.saveBitmap(name, bitmap);
                        }
                        mTakePicCount++;
                        if (mTakePicCount > LIMIT_TAKE_PIC) {
                            ImageUtil.AutoFocusFinishedToKeystone = false;

                            showPattern2();

                            //延时500ms，以免下次拍照拍的还是上次显示的图片
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ImageUtil.KeystonePositiveFinishedToNegative = true;
                                    mTakePicCount = 0;
                                }
                            }, 500);
                        }
                    } else if (ImageUtil.KeystonePositiveFinishedToNegative) {
                        String name = "p0" + (mTakePicCount) + ".png";

                        if (mTakePicCount != 0) {
                            ImageUtil.saveBitmap(name, bitmap);
                        }
                        mTakePicCount++;
                        if (mTakePicCount > LIMIT_TAKE_PIC) {
                            ImageUtil.KeystonePositiveFinishedToNegative = false;
                            // 6. 关闭pattern和摄像头
                            Intent mIntent = new Intent("cvte.intent.action.ProjectorAutoKeystone");
                            mContext.sendBroadcast(mIntent);

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mShowPattern.hidePattern2(mContext);
                                    mCamera2Helper.closeCamera();
                                    isStart = false;
                                    mHandler.removeCallbacks(mOverTimeRunnable);
                                }
                            }, 500);
                        }
                    }

                }
            });
        }

        mCamera2Helper.openCamera(this);
    }

    private void initKeystone() {
        if(SystemPropertiesAdapter.get("persist.cvte.auto_all_correction", "0").equals("1")){
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_lb_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_rb_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_rt_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_lt_offset", "0:1");
        }
    }

    private void removeLocalImgs() {
        File file = new File(ImageUtil.getmSavePath());
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("n0")
                            || f.getName().startsWith("p0")) {
                        f.delete();
                    }
                }
            }
        }
    }

    private void showPattern2() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mShowPattern.showPattern2(mContext);
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground();
        LogUtil.d("onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("onStartCommand " + intent.toString() + " isStart=" + isStart);
        if (isStart) {
            return super.onStartCommand(intent, flags, startId);
        }
        isStart = true;

        mContext = CameraService.this.getApplicationContext();
        mShowPattern = ShowPattern.getInstance(mContext);
        mShowPattern.removeAllView();
        mShowPattern.addView();
        initKeystone();
        removeLocalImgs();
        startForeground();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);

                    //超时退出
                    overtimeExit();

                    if (CVT_EN_AUTO_FOCUS_APPROACH)
                    {
                        // 1. 设置状态机初始状态
                        Log.d("HBK-GAP","GAP-设置状态机初始状态");
                        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_INCREASE;
                        MotorUtil.setMotorIOStartStatus();
                        openCamera();
                        Thread.sleep(500);
                        // 2. 进入逐次逼近状态机，找到最佳位置
                        while(AutoFocusUtil.autoFocusState != AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT) {
                            switch (AutoFocusUtil.autoFocusState) {
                                case AutoFocusUtil.AUTO_FOCUS_INCREASE: {
                                    //跑1s，拍照，计算拉普拉斯值
                                    Log.d("HBK-GAP","GAP-【1】递增");
                                    AutoFocusUtil.setAutoFocusGapTraversal();
                                    break;
                                }
                                case AutoFocusUtil.AUTO_FOCUS_TURN_ROUND: {
                                    Log.d("HBK-GAP","GAP-【2】回转");
                                    //设置方向反向
                                    MotorUtil.setMotorTurnRound();
		                            //跑1s，拍照，计算拉普拉斯值
                                    AutoFocusUtil.setAutoFocusGapTraversal();
                                    break;
                                }
                                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST_CHECK:{
                                    Log.d("HBK-GAP","GAP-【3】找到最清晰检查");
                                    ImageUtil.laplaceBiggestValueCheck = ImageUtil.laplaceBiggestValue;
                                    ImageUtil.laplaceBiggestCountCheck = ImageUtil.laplaceBiggestCount;
                                    ImageUtil.laplaceMaxCountCheck = ImageUtil.laplaceMaxCount;
                                    //设置方向反向
                                    MotorUtil.setMotorTurnRound();
                                    //跑1s，拍照，计算拉普拉斯值
                                    AutoFocusUtil.setAutoFocusGapTraversal();
                                    break;
                                }
                                case AutoFocusUtil.AUTO_FOCUS_TO_CLEAREST:{
                                    Log.d("HBK-GAP","GAP-【4】最清晰位置确认");
                                    //设置方向反向(回转)
                                    MotorUtil.setMotorTurnRound();
                                    //算出最大值的位置，回到最大值，并设置状态机
                                    AutoFocusUtil.setAutoFocusToGapPosition();
                                    break;
                                }
                                default:{
                                    //无状态，退出
                                    AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
                                    break;
                                }
                            }
                        }
                        // 3.清除状态机位置
                        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_NULL;

                    } else {
                        // 1. 将马达转到初始位置
                        AutoFocusUtil.setAutoFocusOrigin();

                        // 2. 马达转动整个过程拍摄所有画面
                        openCamera();

                        AutoFocusUtil.setAutoFocusTraversal();

                        // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
                        AutoFocusUtil.calculateAutoFocusLaplaceMax();

                        // 4. 按比例回转到对应的位置
                        AutoFocusUtil.setAutoFocusToPosition();
                    }

                    // 5. 保存自动校正照片到指定路径
                    mTakePicCount = 0;
                    ImageUtil.AutoFocusFinishedToKeystone = true;


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "AutoFocusThread").start();

        return super.onStartCommand(intent, flags, startId);
    }

    private void overtimeExit() {
        if (mOverTimeRunnable == null) {
            mOverTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    LogUtil.e("over 30 seconds,overtime exit,remove all views!");
                    ShowPattern.getInstance(CameraService.this.getApplicationContext()).removeAllView();
                }
            };
        }
        mHandler.postDelayed(mOverTimeRunnable, 15_000);
    }


    /*****************************************
     * function：灰度化计算清晰度
     * @param srcBitmap 需要计算的图片
     * @return Bitmap
     *****************************************/
    public Bitmap toClarityByOpenCV(Bitmap srcBitmap) {
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
        double value = mean.val[0];
        Log.d("HBK", "Clarity Value:[" + ImageUtil.laplaceCounter + "]" + value);
        Log.d("HBK-GAP", "清晰度:[" + ImageUtil.laplaceCounter + "]" + value);
        ImageUtil.laplaceValue[ImageUtil.laplaceCounter] = value;
        ImageUtil.laplaceCounter = ImageUtil.laplaceCounter + 1;

        Utils.matToBitmap(grayMat, srcBitmap);

        return srcBitmap;
    }

}
