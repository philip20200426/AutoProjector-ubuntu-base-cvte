package com.cvte.camera2demo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static org.opencv.core.Core.mean;

public class CameraService extends Service {
    private Camera2Helper mCamera2Helper;
    private ShowPattern mShowPattern;
    private long mLastTime = -1L;

    private Context mContext;
    Handler mHandler = new Handler();

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

                }
            });
        }

        mCamera2Helper.openCamera(this);
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
                    AutoFocusUtil.saveAutoFocusFinishedToKeystone();

                    // 6. 关闭pattern和摄像头
                    Intent mIntent = new Intent("cvte.intent.action.ProjectorAutoKeystone");
                    mContext.sendBroadcast(mIntent);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mShowPattern.showPattern2(mContext);
//                            mShowPattern.removeView();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mShowPattern.hidePattern2(mContext);
                                }
                            },1000);
                        }
                    });
                    mCamera2Helper.closeCamera();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 9.0以后 START_STICKY不能直接安装
        return START_STICKY;
//        return START_NOT_STICKY;
    }

}
