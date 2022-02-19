package com.cvte.camera2demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.cvte.camera2demo.util.LogUtil;

import java.io.File;
import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

public class CameraService extends Service {
    private Camera2Helper mCamera2Helper;
    private long mLastTime = -1L;

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


    @Override
    public void onCreate() {
        super.onCreate();

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

        // 9.0以后 START_STICKY不能直接安装
        return START_STICKY;
//        return START_NOT_STICKY;
    }


}
