package com.cvte.camera2demo;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static com.cvte.camera2demo.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;
import static org.opencv.core.Core.mean;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.at.platform.AtShellCmd;
import com.cvte.camera2demo.util.AutoFocusUtil;
import com.cvte.camera2demo.util.ImageUtil;
import com.cvte.camera2demo.util.LogUtil;
import com.cvte.camera2demo.util.MotorUtil;
import com.cvte.camera2demo.util.PatternManager;
import com.cvte.camera2demo.util.SaveKeystoneUtil;
import com.cvte.camera2demo.util.ShellCmd;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class CameraService extends Service {
    private static final String TAG = CameraService.class.getName();
    private Camera2Helper mCamera2Helper;
    //    private ShowPattern mShowPattern;
    private long mLastTime = -1L;

    private Context mContext;
    Handler mHandler = new Handler();
    private int mTakePicCount = 0;
    private final int LIMIT_TAKE_PIC = 1;
    private boolean isStart = false;
    private Runnable mOverTimeRunnable;
    /**
     * 纯图像，逐次逼近算法
     */
    public static final Boolean CVT_EN_AUTO_FOCUS_APPROACH = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_AUTO_FOCUS_APPROACH", false);

    /**
     * 纯步数和限位UEvent，逐次逼近算法
     */
    public static final Boolean CVT_EN_KEYSTONE_TWO_PATTERN = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_KEYSTONE_TWO_PATTERN", false);
    /**
     * 纯步数和限位UEvent，逐次逼近算法
     */
    public static final Boolean CVT_EN_AUTO_FOCUS_BORDER_CHECK = SystemPropertiesAdapter.getBoolean("persist.focus.border.check", false);
    /**
     * 处理自动对焦相关耗时任务
     */
    private Looper autoFocusLooper;
    private CameraServiceHandler autoFocusHandler;
    /**
     * ui更新相关
     */
    private CameraServiceHandler mUIHandler;

    /**
     * 开始对焦
     */
    private static final int START_AUTO_FOCUS = 1001;

    private static final int SHOW_PATTERN1 = 1002;

    private static final int SHOW_PATTERN2 = 1003;

    private static final int REMOVE_PATTERN = 1004;
    /**
     * 完成对焦
     */
    private static final int FINISH_AUTO_FOCUS = 1005;

    /**
     * 对焦超时
     */
    private static final int TIME_OUT_AUTO_FOCUS = 1006;
    /**
     * 对焦完成，保存图片
     */
    private static final int AUTO_FOCUS_FINISHED_TO_KEYSTONE = 1007;
    /**
     * Keystone 正面完成到负面
     */
    private static final int KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE = 1008;
    /**
     * 发送imu广播
     */
    private static final int BROADCAST_SAVE_IMU_DATA = 1009;
    /**
     * 发送梯形校正广播
     */
    private static final int BROADCAST_PROJECTOR_AUTO_KEYSTONE = 10010;
    /**
     * 矫正刷新
     */
    private static final int BROADCAST_KEYSTONE_FINISH_TO_REFRESH = 10011;

    private PatternManager patternManager;

    /**
     * keystoneRefreshTimes 梯形校正次数
     */
    private int keystoneRefreshTimes = 0;

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
        HandlerThread autoFocusThread = new HandlerThread("autoFocusThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        autoFocusThread.start();
        autoFocusHandler = new CameraServiceHandler(autoFocusThread.getLooper());
        mUIHandler = new CameraServiceHandler(Looper.getMainLooper());

        if (!OpenCVLoader.initDebug()) {
            Log.d("HBK", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("HBK", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            LogUtil.d("onStartCommand " + intent.toString() + " isStart=" + isStart);
        }
        LogUtil.d("auto_focus:" + SystemPropertiesAdapter.get("persist.cvte.auto_focus", "1"));
        //设置 关闭 开机启动自动对焦 操作在tvAPI中间件进行
//        if (isStart || SystemPropertiesAdapter.get("persist.cvte.auto_focus", "1").equals("0")) {
//            return super.onStartCommand(intent, flags, startId);
//        }
        isStart = true;
        MotorUtil.initStepMotorStatus();
        mContext = CameraService.this.getApplicationContext();
        patternManager = new PatternManager(mContext);
        mUIHandler.sendEmptyMessage(SHOW_PATTERN1);
        initKeystone();
        removeLocalImgs();
        startForeground();
        autoFocusHandler.sendEmptyMessageDelayed(START_AUTO_FOCUS, 300);
        return super.onStartCommand(intent, flags, startId);
    }

    public void openCamera() {
        if (mCamera2Helper == null) {
            mCamera2Helper = new Camera2Helper(new Camera2Helper.OnCameraListener() {
                @Override
                public void onCameraStarted() {
                    removeLocalImgs();
                }


                @Override
                public void onCameraError(int error) {
                    mUIHandler.sendEmptyMessage(REMOVE_PATTERN);
                }

                @Override
                public void onCaptureComplete(Bitmap bitmap, File file) {
                    long now = SystemClock.uptimeMillis();
                    if (mLastTime != -1) {
                        Log.d(TAG, " get bitmap duration = " + (now - mLastTime));
                    }
                    mLastTime = now;
                    if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                        //保存图片到运存上的数据池
                        if (beginTakePhoto()) {
                            saveBitmapToDataPoolOnRAM(bitmap);
                        }
                    } else {
                        // 计算拉普拉斯清晰度
//                        toClarityByOpenCV(bitmap);
                        toClarityByOpenCVWithNoGray(bitmap);
                    }
                    //保存自动校正需要的图片到本地
                    saveAutoKeystonePhoto(bitmap);
                }
            });
        }

        mCamera2Helper.openCamera(this);
    }

    private boolean beginTakePhoto() {
        boolean ret = SystemPropertiesAdapter.get(PERSIST_BEGIN_TAKE_PHOTO, "0").equals("1");
        return ret;
    }

    private void saveBitmapToDataPoolOnRAM(Bitmap bitmap) {
        if (ImageUtil.bitmapPoolCounter <= ImageUtil.BITMAP_MAX_COUNT) {
            ImageUtil.bitmapPool[ImageUtil.bitmapPoolCounter] = bitmap;
            ImageUtil.bitmapPoolCounter++;
        }
    }

    /**
     * 保存自动校正需要的图片到本地
     *
     * @param bitmap bitmap
     */
    private void saveAutoKeystonePhoto(Bitmap bitmap) {
        // 保存到本地
        if (ImageUtil.AutoFocusFinishedToKeystone) {
            String name = "n0" + (mTakePicCount) + ".png";
            if (mTakePicCount != 0) {
                ImageUtil.saveBitmap(name, bitmap);
            }
            mTakePicCount++;
            if (mTakePicCount > LIMIT_TAKE_PIC) {
                ImageUtil.AutoFocusFinishedToKeystone = false;
                if (CVT_EN_KEYSTONE_TWO_PATTERN) {
                    switchToPatternTwo();
                } else {
                    finishAutoFocusService();
                }
            }
        } else if (ImageUtil.KeystonePositiveFinishedToNegative) {
            String name = "p0" + (mTakePicCount) + ".png";
            if (mTakePicCount != 0) {
                ImageUtil.saveBitmap(name, bitmap);
            }
            mTakePicCount++;
            if (mTakePicCount > LIMIT_TAKE_PIC) {
                ImageUtil.KeystonePositiveFinishedToNegative = false;
                finishAutoFocusService();
            }
        }
    }

    private void switchToPatternTwo() {
        mUIHandler.sendEmptyMessage(SHOW_PATTERN2);
        //延时500ms，以免下次拍照拍的还是上次显示的图片
        mUIHandler.sendEmptyMessageDelayed(KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE, 1000);
    }

    private void finishAutoFocusService() {
        LogUtil.d("结束自动对焦，关闭pattern和摄像头" + Thread.currentThread().getName());
//        AtShellCmd.Sudo("su");
//        AtShellCmd.Sudo("xu 7411");
//        AtShellCmd.Sudo("setenforce 0");
        mUIHandler.sendEmptyMessageDelayed(BROADCAST_PROJECTOR_AUTO_KEYSTONE, 0);
        reopenAsuSpeech();
        mUIHandler.sendEmptyMessageDelayed(FINISH_AUTO_FOCUS, 0);
    }

    private void closeAsuSpeech() {
        int asuSpeech = Settings.Global.getInt(mContext.getContentResolver(), "settings_asu_speech_enabled", 1);
        if (asuSpeech == 1) {
            Settings.Global.putInt(mContext.getContentResolver(), "settings_asu_speech_enabled", 0);
        }
    }

    private void reopenAsuSpeech() {
        int asuSpeech = Settings.Global.getInt(mContext.getContentResolver(), "settings_asu_speech_enabled", 0);
        if (asuSpeech == 0) {
            Settings.Global.putInt(mContext.getContentResolver(), "settings_asu_speech_enabled", 1);
        }
    }

    private void initKeystone() {
        if (SystemPropertiesAdapter.get("persist.cvte.auto_all_correction", "0").equals("1")) {
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_lb_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_rb_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_rt_offset", "0:0");
            SystemPropertiesAdapter.set("vendor.mstar.test.pos_lt_offset", "0:1");
            SaveKeystoneUtil.saveUserDataToSystem();
        }
        if (SystemPropertiesAdapter.get("persist.sys.auto_foucs", "0").equals("1")) {
            SystemPropertiesAdapter.set("persist.vendor.hwc.keystone", "0.0,0.0,1920.0,0.0,1920,1080,0,1080.0");
            ShellCmd.exec("service call SurfaceFlinger 1006");
        }

        //init Border Check
        if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
            Log.d("HBK-U", "initKeystone");
            BorderCheckReceiver.getInstance().startObserving();
        }
        closeAsuSpeech();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground();
        //init Border Check
        if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
            BorderCheckReceiver.getInstance().stopObserving();
        }
        LogUtil.d("onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sendMessageForSaveIMUData() {
        Intent mIntent = new Intent("cvte.intent.action.GsensorStoreOnlineImu");
        mContext.sendBroadcast(mIntent);
    }

    /**
     * 1.转动马达到初始位置
     * 2.拍照
     * 3.查找清晰
     * 4.转动马达到清晰地方
     */
    public void autoFocusTraversingMethod() {
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

    /**
     * 超时退出
     */
    private void overtimeExit() {
        LogUtil.e("over 30 seconds,overtime exit,remove all views!");
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "999");
//        ImageUtil.resetBitmapPool();
        mUIHandler.sendEmptyMessage(REMOVE_PATTERN);
        mUIHandler.sendEmptyMessageDelayed(FINISH_AUTO_FOCUS, 0);
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
        Log.d("HBK-GAP", "清晰度:[" + ImageUtil.laplaceCounter + "]" + value);
        ImageUtil.laplaceValue[ImageUtil.laplaceCounter] = value;
        ImageUtil.laplaceCounter = ImageUtil.laplaceCounter + 1;

        Utils.matToBitmap(grayMat, srcBitmap);

        return srcBitmap;
    }

    /*****************************************
     * function：计算清晰度（无gray转化，因为投影摄像头默认是黑白的）
     * @param srcBitmap 需要计算的图片
     * @return Bitmap
     *****************************************/
    public void toClarityByOpenCVWithNoGray(Bitmap srcBitmap) {
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
        double value = mean.val[0];
        Log.d("HBK-GAP", "清晰度:[" + ImageUtil.laplaceCounter + "]" + value);
        ImageUtil.laplaceValue[ImageUtil.laplaceCounter] = value;
        ImageUtil.laplaceCounter = ImageUtil.laplaceCounter + 1;
    }

    public static Mat cutImgROI(Mat bitmap) {
        int startRow = 232, endRow = 232 + 256;
        int startCol = 512, endCol = 512 + 256;
        Range areaRow = new Range(startRow, endRow);
        Range areaCol = new Range(startCol, endCol);
        return new Mat(bitmap, areaRow, areaCol);
    }

    /**
     * 自动对焦业务处理
     */
    private final class CameraServiceHandler extends Handler {
        public CameraServiceHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case START_AUTO_FOCUS:
                    try {
                        Log.d(TAG, "start autoFocus:" + Thread.currentThread().getName());
                        //超时退出
                        mUIHandler.sendEmptyMessageDelayed(TIME_OUT_AUTO_FOCUS, 30_000);
                        Log.d(TAG, "CVT_EN_AUTO_FOCUS_APPROACH:" + CVT_EN_AUTO_FOCUS_APPROACH);
                        Log.d(TAG, "CVT_EN_AUTO_FOCUS_BORDER_CHECK:" + CVT_EN_AUTO_FOCUS_BORDER_CHECK);
                        if (CVT_EN_AUTO_FOCUS_APPROACH) {
                            /*AutoFocusMethod② 纯图像，逐次逼近算法*/
                            openCamera();
                            AutoFocusMethod.autoFocusStepSuccessiveApproximation();
                        } else if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                            /*AutoFocusMethod③ 纯步数和限位UEvent，逐次逼近算法*/
                            openCamera();
//                            ImageUtil.resetBitmapPool();
                            AutoFocusMethod.autoFocusStepBorderCheckFunc();
                        } else {
                            /*AutoFocusMethod① 纯图像，时间遍历算法*/
                            autoFocusTraversingMethod();
                        }
                        mUIHandler.sendEmptyMessage(BROADCAST_SAVE_IMU_DATA);
                        // 5. 保存自动校正照片到指定路径
                        mTakePicCount = 0;
                        mUIHandler.sendEmptyMessageDelayed(AUTO_FOCUS_FINISHED_TO_KEYSTONE, 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case SHOW_PATTERN1:
                    Log.d(TAG, "show_pattern--" + Thread.currentThread().getName());
                    patternManager.showPattern();
                    break;
                case SHOW_PATTERN2:
                    Log.d(TAG, "show_pattern2--" + Thread.currentThread().getName());
                    patternManager.showPattern2();
                    break;
                case REMOVE_PATTERN:
                    patternManager.removeAllView();
                    break;
                case TIME_OUT_AUTO_FOCUS:
                    Log.d(TAG, "timeOut autoFocus--" + Thread.currentThread().getName());
                    overtimeExit();
                    break;
                case AUTO_FOCUS_FINISHED_TO_KEYSTONE:
                    Log.d(TAG, "finish autoFocus to keystone--" + Thread.currentThread().getName());
                    ImageUtil.AutoFocusFinishedToKeystone = true;
                    break;
                case KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE:
                    Log.d(TAG, "keystone positive finished to negative--" + Thread.currentThread().getName());
                    ImageUtil.KeystonePositiveFinishedToNegative = true;
                    mTakePicCount = 0;
                    break;
                case FINISH_AUTO_FOCUS:
                    Log.d(TAG, "finish autoFocus--" + Thread.currentThread().getName());
                    patternManager.removeAllView();
                    mCamera2Helper.closeCamera();
                    isStart = false;
                    mHandler.removeCallbacks(mOverTimeRunnable);
                    if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                        BorderCheckReceiver.getInstance().stopObserving();
                    }
                    stopSelf();
                    System.exit(0);
                    break;
                case BROADCAST_SAVE_IMU_DATA:
                    // IMU:发送广播让系统存校准后的IMU数据
                    sendMessageForSaveIMUData();
                    break;
                case BROADCAST_PROJECTOR_AUTO_KEYSTONE:
                    // 6. 关闭pattern和摄像头
                    Intent mIntent = new Intent("cvte.intent.action.ProjectorAutoKeystone");
                    mContext.sendBroadcast(mIntent);
                    //延时1s，以免上面imu数据没写完
//                    AtShellCmd.Sudo("/vendor/bin/main -6 3000");
//                    autoFocusHandler.sendEmptyMessageDelayed(BROADCAST_KEYSTONE_FINISH_TO_REFRESH, 1500);
                    break;
                case BROADCAST_KEYSTONE_FINISH_TO_REFRESH:
                    AtShellCmd.Sudo("su");
                    AtShellCmd.Sudo("xu 7411");
                    AtShellCmd.Sudo("setenforce 0");
                    AtShellCmd.Sudo("service call SurfaceFlinger 1006");
                    AtShellCmd.Sudo("sync");
                    Log.d(TAG, "keystoneRefreshTimes: " + keystoneRefreshTimes);
                    if (keystoneRefreshTimes < 10) {
                        autoFocusHandler.sendEmptyMessageDelayed(BROADCAST_KEYSTONE_FINISH_TO_REFRESH, 1500);
                        keystoneRefreshTimes++;
                    }
                    break;
                default:
                    Log.d(TAG, "handleMessage: " + Thread.currentThread().getName());
                    break;
            }
        }
    }
}
