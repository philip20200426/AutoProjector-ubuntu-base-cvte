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

import com.cvte.camera2demo.util.AutoFocusUtil;
import com.cvte.camera2demo.util.ImageUtil;
import com.cvte.camera2demo.util.LogUtil;

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
    private final int LIMIT_TAKE_PIC = 5;

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

                }


                @Override
                public void onCameraError(int error) {

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
                        String name = "n0" + (mTakePicCount) + ".bmp";
                        if (mTakePicCount != 0) {
                            ImageUtil.saveBmp(name, bitmap);
                        }
                        mTakePicCount++;
                        if (mTakePicCount > LIMIT_TAKE_PIC) {
                            ImageUtil.AutoFocusFinishedToKeystone = false;

                            showPattern2();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ImageUtil.KeystonePositiveFinishedToNegative = true;
                                    mTakePicCount = 0;
                                }
                            }, 500);
                        }
                    } else if (ImageUtil.KeystonePositiveFinishedToNegative) {
                        String name = "p0" + (mTakePicCount) + ".bmp";

                        if (mTakePicCount != 0) {
                            ImageUtil.saveBmp(name, bitmap);
                        }
                        mTakePicCount++;
                        if (mTakePicCount > LIMIT_TAKE_PIC) {
                            ImageUtil.KeystonePositiveFinishedToNegative = false;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // 6. 关闭pattern和摄像头
                                    Intent mIntent = new Intent("cvte.intent.action.ProjectorAutoKeystone");
                                    mContext.sendBroadcast(mIntent);


                                    mShowPattern.hidePattern2(mContext);
                                    mCamera2Helper.closeCamera();
                                }
                            }, 9000);
                        }
                    }

                }
            });
        }

        mCamera2Helper.openCamera(this);
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this.getApplicationContext();
        mShowPattern = ShowPattern.getInstance(mContext);
        mShowPattern.removeAllView();
        mShowPattern.addView();
        startForeground();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                    // 1. 将马达转到初始位置
                    AutoFocusUtil.setAutoFocusOrigin();

                    // 2. 马达转动整个过程拍摄所有画面
                    openCamera();
                    AutoFocusUtil.setAutoFocusTraversal();

                    // 3. 找出清晰度最大值下标以及总共有多少张图片，并计算得出最清晰（最大值）的画面时间在整个马达转动过程中的哪个位置
                    AutoFocusUtil.calculateAutoFocusLaplaceMax();

                    // 4. 按比例回转到对应的位置
                    AutoFocusUtil.setAutoFocusToPosition();

                    // 5. 保存自动校正照片到指定路径
                    mTakePicCount = 0;
                    ImageUtil.AutoFocusFinishedToKeystone = true;


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 9.0以后 START_STICKY不能直接安装
//        return START_STICKY;
        return START_NOT_STICKY;
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
        Log.d("HBK", "Clarity Value:" + value);
        ImageUtil.laplaceValue[ImageUtil.laplaceCounter] = value;
        ImageUtil.laplaceCounter = ImageUtil.laplaceCounter + 1;

        Utils.matToBitmap(grayMat, srcBitmap);

        return srcBitmap;
    }

}
