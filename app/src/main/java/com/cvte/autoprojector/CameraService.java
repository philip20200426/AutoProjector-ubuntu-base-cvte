package com.cvte.autoprojector;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static com.cvte.autoprojector.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;
import static com.cvte.autoprojector.util.Constants.PERSIST_FINISH_TAKE_PHOTO;
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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;
import com.cvte.at.platform.AtShellCmd;
import com.cvte.autoprojector.util.AutoFocusUtil;
import com.cvte.autoprojector.util.FileUtil;
import com.cvte.autoprojector.util.ImageUtil;
import com.cvte.autoprojector.util.LogUtil;
import com.cvte.autoprojector.util.MotorUtil;
import com.cvte.autoprojector.util.OpenCVUtils;
import com.cvte.autoprojector.util.PatternManager;
import com.cvte.autoprojector.util.SaveKeystoneUtil;
import com.cvte.autoprojector.util.ShellCmd;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class CameraService extends Service implements CvCameraViewListener2{
    private static final String TAG = CameraService.class.getName();
    private Camera2Helper mCamera2Helper;
    private MotorHelper mMotorHelper;
    //    private ShowPattern mShowPattern;
    private long mLastTime = -1L;

    private Context mContext;
    private int mTakePicCount = 0;
    private final int LIMIT_TAKE_PIC = 1;
    private boolean isStart = false;
    //(??????home?????????????????????0?????????????????????,1?????????????????????,2???????????????????????????)
    private String OPERATION_LONG_PRESS_HOME = "";
    private Runnable mOverTimeRunnable;
    private CameraBridgeViewBase mOpenCvCameraView;
    /**
     * ??????????????????????????????
     */
    public static final Boolean CVT_EN_AUTO_FOCUS_APPROACH = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_AUTO_FOCUS_APPROACH", false);

    /**
     * ??????????????????UEvent?????????????????????
     */
    public static final Boolean CVT_EN_KEYSTONE_TWO_PATTERN = SystemPropertiesAdapter.getBoolean("ro.CVT_EN_KEYSTONE_TWO_PATTERN", false);
    /**
     * ??????????????????UEvent?????????????????????
     */
    public static final Boolean CVT_EN_AUTO_FOCUS_BORDER_CHECK = SystemPropertiesAdapter.getBoolean("persist.focus.border.check", false);
    /**
     * ????????????????????????????????????
     */
    private CameraServiceHandler autoFocusHandler;
    /**
     * ui????????????
     */
    private CameraServiceHandler mUIHandler;

    /**
     * ????????????
     */
    private static final int START_AUTO_FOCUS = 1001;

    private static final int SHOW_PATTERN1 = 1002;

    private static final int SHOW_PATTERN2 = 1003;

    private static final int REMOVE_PATTERN = 1004;
    /**
     * ????????????
     */
    private static final int FINISH_AUTO_FOCUS = 1005;

    /**
     * ????????????
     */
    private static final int TIME_OUT_AUTO_FOCUS = 1006;
    /**
     * ???????????????????????????
     */
    private static final int AUTO_FOCUS_FINISHED_TO_KEYSTONE = 1007;
    /**
     * Keystone ?????????????????????
     */
    private static final int KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE = 1008;
    /**
     * ??????imu??????
     */
    private static final int BROADCAST_SAVE_IMU_DATA = 1009;
    /**
     * ????????????????????????
     */
    private static final int BROADCAST_PROJECTOR_AUTO_KEYSTONE = 10010;
    /**
     * ????????????
     */
    private static final int BROADCAST_KEYSTONE_FINISH_TO_REFRESH = 10011;

    private static final int SHOW_BLANK_PATTERN = 10012;

    private static final int TAKE_PICTURE_DELAY_MS = 1000;
    private static int mCount = 0;

    private PatternManager patternManager;

    /**
     * keystoneRefreshTimes ??????????????????
     */
    private int keystoneRefreshTimes = 0;

    private ImageManager imageManager;

    private ViewGroup mWindowContainer;
    private static long pre_now=0;

    public CameraService() {
    }

    @Override
    public void onCameraViewStarted(int i, int i1) {
        Log.d("philip", "onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d("philip", "--------------------------------");
        return inputFrame.rgba();
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
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("HBK", "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                    break;
                default:
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

/*        mWindowContainer = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.showpattern2, null);
        mOpenCvCameraView = mWindowContainer.findViewById(R.id.opencv_surface_view);*/

         //mOpenCvCameraView = new JavaCameraView(this, 1);
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
            //??????????????????home????????????????????????intent???????????????,0- focus 1-keystone 2- focus and keystoen
            OPERATION_LONG_PRESS_HOME = intent.getStringExtra("action");
            LogUtil.d("onStartCommand OPERATION_LONG_PRESS_HOME" + OPERATION_LONG_PRESS_HOME);
            LogUtil.d("onStartCommand " + intent.toString() + " isStart=" + isStart);
        }
        LogUtil.d("auto_focus:" + SystemPropertiesAdapter.get("persist.cvte.auto_focus", "1"));
        //?????? ?????? ???????????????????????? ?????????tvAPI???????????????
        if (!isStart) {
            mContext = CameraService.this.getApplicationContext();
            startForeground();
            MotorUtil.initStepMotorStatus();
            initKeystone();
            patternManager = new PatternManager(mContext);
            mUIHandler.sendEmptyMessage(SHOW_PATTERN1);
            imageManager = new ImageManager();
            openCamera();
            isStart = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    boolean isReadyBlank = false;

    public void openCamera() {
        if (mCamera2Helper == null) {
            mCamera2Helper = new Camera2Helper(new Camera2Helper.OnCameraListener() {
                @Override
                public void onCameraStarted() {
//                    removeLocalImgs();
                    Log.d(TAG, "onCameraStarted");
                    autoFocusHandler.sendEmptyMessageDelayed(START_AUTO_FOCUS, 300);
                }


                @Override
                public void onCameraError(int error) {
                    mUIHandler.sendEmptyMessage(REMOVE_PATTERN);
                }

                @Override
                public void onCaptureComplete(Bitmap bitmap, File file, long duration, long frameId) {
                    if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                        //????????????????????????????????????
                        //if (beginTakePhoto())
                        {
//                            saveBitmapToDataPoolOnRAM(bitmap);
                            ImageBean imageBean = new ImageBean();
                            imageBean.setFrameId(frameId);
                            imageBean.setIndex(imageManager.getImageSize());
                            imageBean.setBitmap(bitmap);
                            imageBean.setDuration(duration);
                            imageBean.setLaplacian(imageManager.calculateOnePhotoClarity(bitmap));
                            imageManager.addImage(imageBean);
                            //ImageUtil.saveBitmap(String.valueOf(imageManager.getImageSize()), bitmap);
                            Log.d("philip", "FrameID :  X "+frameId);
                            imageManager.decreaseImageCount();
  /*                          mCount++;
                            if (mCount == imageManager.getmLocation()) {
                                imageManager.calculateBitmapPoolLaplaceMax();
                                for (int j = 0; j < imageManager.getImageSize(); j++) {
                                    Log.d("philip", "j "+j+" current position lapulasi " + imageManager.getImageList().get(j).getLaplacian() +
                                            " size " + imageManager.getImageSize());
                                }
                                FileUtil.exportCsv("/data/test.csv", imageManager);
                                SystemPropertiesAdapter.set(PERSIST_FINISH_TAKE_PHOTO, "1");
                                //FileUtil.readCSV("/data/test.csv");
                                mCount = 0;
                            }*/
/*                            long now = SystemClock.uptimeMillis();
                            Log.d("philip", "ladency : " + (now-pre_now) + " duration : "+ duration + " mCount : " + mCount);
                            pre_now = now;*/
                        }
                    } /*else {
                        long now = SystemClock.uptimeMillis();
                        Log.d("philip", "ladency : " + (now-pre_now));
                        pre_now = now;
*//*                        // ???????????????????????????
//                        toClarityByOpenCV(bitmap);
                        toClarityByOpenCVWithNoGray(bitmap);*//*
                    }*/
                    //??????????????????????????????????????????
                    saveAutoKeystonePhoto(bitmap);
                    if (isReadyBlank) {
                        ImageUtil.saveBlankBitmap("white_tmp.png", bitmap);
                        mUIHandler.sendEmptyMessageDelayed(BROADCAST_PROJECTOR_AUTO_KEYSTONE, 0);
                    }
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
        if (ImageUtil.bitmapPoolLength <= ImageUtil.BITMAP_MAX_COUNT) {
            ImageUtil.bitmapPool[ImageUtil.bitmapPoolLength] = bitmap;
            ImageUtil.bitmapPoolLength++;
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param bitmap bitmap
     */
    private void saveAutoKeystonePhoto(Bitmap bitmap) {
        // ???????????????
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
    }

    private void finishAutoFocusService() {
        Log.d(TAG,"???????????????????????????pattern????????????" + Thread.currentThread().getName());
//        AtShellCmd.Sudo("su");
//        AtShellCmd.Sudo("xu 7411");
//        AtShellCmd.Sudo("setenforce 0");

        reopenAsuSpeech();

        if (isOpenBlank()) {
            mUIHandler.sendEmptyMessage(SHOW_BLANK_PATTERN);
        }
        mUIHandler.sendEmptyMessageDelayed(FINISH_AUTO_FOCUS, 2000);
    }

    /**
     * ????????????????????????????????????zhang
     *
     * @return
     */
    private boolean isOpenBlank() {
        return SystemPropertiesAdapter.get("persist.cvte.auto.obstacle.avoidance","0").equals("1");
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
            //BorderCheckReceiver.getInstance().startObserving();
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
            //BorderCheckReceiver.getInstance().stopObserving();
        }
        if (mUIHandler != null) {
            mUIHandler.removeCallbacksAndMessages(null);
        }
        if (autoFocusHandler != null) {
            autoFocusHandler.removeCallbacksAndMessages(null);
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
     * 1.???????????????????????????
     * 2.??????
     * 3.????????????
     * 4.???????????????????????????
     */
    public void autoFocusTraversingMethod() {
        // 1. ???????????????????????????
        AutoFocusUtil.setAutoFocusOrigin();

        // 2. ??????????????????????????????????????????
//        openCamera();

        AutoFocusUtil.setAutoFocusTraversal();

        // 3. ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        AutoFocusUtil.calculateAutoFocusLaplaceMax();

        // 4. ?????????????????????????????????
        AutoFocusUtil.setAutoFocusToPosition();
    }

    /**
     * ????????????
     */
    private void overtimeExit() {
        LogUtil.e("over 30 seconds,overtime exit,remove all views!");
        AutoFocusUtil.autoFocusState = AutoFocusUtil.AUTO_FOCUS_FINISHED_TO_EXIT;
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "999");
//        ImageUtil.resetBitmapPool();
        mUIHandler.sendEmptyMessage(REMOVE_PATTERN);
        mUIHandler.sendEmptyMessageDelayed(FINISH_AUTO_FOCUS, 50);
    }


    /*****************************************
     * function???????????????????????????
     * @param srcBitmap ?????????????????????
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
        Log.d("HBK-GAP", "?????????:[" + ImageUtil.laplaceCounter + "]" + value);
        ImageUtil.laplaceValue[ImageUtil.laplaceCounter] = value;
        ImageUtil.laplaceCounter = ImageUtil.laplaceCounter + 1;

        Utils.matToBitmap(grayMat, srcBitmap);

        return srcBitmap;
    }

    /*****************************************
     * function????????????????????????gray???????????????????????????????????????????????????
     * @param srcBitmap ?????????????????????
     *
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
        Log.d("HBK-GAP", "?????????:[" + ImageUtil.laplaceCounter + "]" + value);
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

    private long startAutoFocusTime;

    /**
     * ????????????????????????
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
                        startAutoFocusTime = SystemClock.uptimeMillis();
                        Log.d(TAG, "philip start autoFocus" + "a:" + CVT_EN_AUTO_FOCUS_APPROACH + "b" + CVT_EN_AUTO_FOCUS_BORDER_CHECK);
                        //????????????
                        //mUIHandler.sendEmptyMessageDelayed(TIME_OUT_AUTO_FOCUS, 70000);
                        if (CVT_EN_AUTO_FOCUS_APPROACH) {
                            /*AutoFocusMethod??? ??????????????????????????????*/
//                            openCamera();
                            AutoFocusMethod.autoFocusStepSuccessiveApproximation();
                        } else if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                            /*AutoFocusMethod??? ??????????????????UEvent?????????????????????*/
//                            openCamera();
                            //AutoFocusMethod.autoFocusStepBorderCheckFunc(imageManager);
                            //AutoFocusMethod.autoFocusTest(imageManager);
                            //AutoFocusMethod.fineSearchAF(imageManager);
                            //AutoFocusMethod.dataCaptureAF(imageManager);
                            AutoFocusManager.climbingMethodAF(imageManager);
                        } else {
                            /*AutoFocusMethod??? ??????????????????????????????*/
                            autoFocusTraversingMethod();
                        }
                        //??????imu??????
                        mUIHandler.sendEmptyMessage(BROADCAST_SAVE_IMU_DATA);
                        // 5. ???????????????????????????????????????
//                        mTakePicCount = 0;
                        //??????????????????
                        mUIHandler.sendEmptyMessage(FINISH_AUTO_FOCUS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case SHOW_BLANK_PATTERN:
                    Log.d(TAG, "show_pattern  showBlankPattern-- ");
                    patternManager.showBlankPattern();
                    patternManager.removeAllView();
                    mUIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "show_pattern  isReadyBlank-- ");
                            isReadyBlank = true;
                        }
                    }, TAKE_PICTURE_DELAY_MS);
                    break;
                case SHOW_PATTERN1:
                    Log.d(TAG, "show_pattern--");
                    patternManager.showPattern();
                    break;
                case SHOW_PATTERN2:
                    Log.d(TAG, "show_pattern2--");
                    patternManager.showPattern2();
                    //??????1000ms??????????????????????????????????????????????????????
                    mUIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ImageUtil.KeystonePositiveFinishedToNegative = true;
                        }
                    }, TAKE_PICTURE_DELAY_MS);
//                    mTakePicCount = 0;
//                    mUIHandler.sendEmptyMessageDelayed(KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE, 1000);
                    break;
                case REMOVE_PATTERN:
                    Log.d(TAG, "remove_pattern--");
                    patternManager.removeAllView();
                    break;
                case TIME_OUT_AUTO_FOCUS:
                    Log.d(TAG, "timeOut autoFocus--");
                    overtimeExit();
                    break;
                case AUTO_FOCUS_FINISHED_TO_KEYSTONE:
                    Log.d(TAG, "finish autoFocus to keystone--");
                    ImageUtil.AutoFocusFinishedToKeystone = true;
                    break;
                case KEYSTONE_POSITIVE_FINISHED_TO_NEGATIVE:
                    Log.d(TAG, "keystone positive finished to negative--");
                    ImageUtil.KeystonePositiveFinishedToNegative = true;
//                    mTakePicCount = 0;
                    break;
                case FINISH_AUTO_FOCUS:
                    Log.d(TAG, "end autoFocus-?????????" + (SystemClock.uptimeMillis() - startAutoFocusTime));
                    patternManager.removeAllView();
                    mCamera2Helper.closeCamera();
                    isStart = false;
                    if (CVT_EN_AUTO_FOCUS_BORDER_CHECK) {
                        BorderCheckReceiver.getInstance().stopObserving();
                    }
                    stopSelf();
                    System.exit(0);
                    break;
                case BROADCAST_SAVE_IMU_DATA:
                    Log.d(TAG, "send cvte.intent.action.GsensorStoreOnlineImu");
                    // IMU:????????????????????????????????????IMU??????
                    sendMessageForSaveIMUData();
                    break;
                case BROADCAST_PROJECTOR_AUTO_KEYSTONE:
                    Log.d(TAG, "send cvte.intent.action.ProjectorAutoKeystone");
                    Intent mIntent = new Intent("cvte.intent.action.ProjectorAutoKeystone");
                    mIntent.putExtra("action", OPERATION_LONG_PRESS_HOME);
                    mContext.sendBroadcast(mIntent);
                    //??????1s???????????????imu???????????????
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
                    Log.d(TAG, "unknown command " + Thread.currentThread().getName());
                    break;
            }
        }
    }
}
