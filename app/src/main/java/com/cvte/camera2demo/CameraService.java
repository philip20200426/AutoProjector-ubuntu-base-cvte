package com.cvte.camera2demo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
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

import com.cvte.camera2demo.util.LogUtil;

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
    private long mLastTime = -1L;
    public static final String MANUAL_FOCUS_IO_FOREWORD = "/sys/class/gpio/gpio39/value";
    public static final String MANUAL_FOCUS_IO_BACKWARD = "/sys/class/gpio/gpio40/value";
    public static final String MANUAL_FOCUS_IO_FOREWORD_ON = "1";
    public static final String MANUAL_FOCUS_IO_FOREWORD_OFF = "0";
    public static final String MANUAL_FOCUS_IO_BACKWARD_ON = "1";
    public static final String MANUAL_FOCUS_IO_BACKWARD_OFF = "0";
    double value;

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

        startForeground();

        openCamera();

        //1.开始转动马达
        //foreword
        writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_ON);
        writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);

        Handler handler = new Handler();
        handler.postDelayed(new closehandler(), 5000); // 延迟2秒，closehandler()

        // 9.0以后 START_STICKY不能直接安装
        return START_STICKY;
//        return START_NOT_STICKY;
    }

    class closehandler implements Runnable{
        public void run() {
            //1.停止转动马达
            //stop
            writeSys(MANUAL_FOCUS_IO_FOREWORD, MANUAL_FOCUS_IO_FOREWORD_OFF);
            writeSys(MANUAL_FOCUS_IO_BACKWARD, MANUAL_FOCUS_IO_BACKWARD_OFF);

            //2.关闭摄像头
            mCamera2Helper.closeCamera();
        }
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
