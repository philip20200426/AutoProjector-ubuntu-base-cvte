package com.cvte.camera2demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.LightingColorFilter;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;


import com.cvte.camera2demo.util.ImageUtil;
import com.cvte.camera2demo.util.LogUtil;
import com.cvte.camera2demo.util.MotorUtil;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.opencv.core.Core.mean;

public class Camera2Helper implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "Camera2Helper";
    private static final int SUPPORT_WIDTH_MIN = 1280;
    public static int SIZE_WIDTH = 1920;
    public static int SIZE_HEIGHT = 1080;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private HandlerThread mProcessThread;
    private Handler mProcessHandler;

    public Size mPreviewSize;

    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private Range<Integer>[] mFpsRanges;

    private byte[] mBuffer;
    private ImageReader mImageReader;
    public SurfaceTexture mSurfaceTexture;

    private int mPreviewWidth;
    private int mPreviewHeight;

    private boolean mOpenDebug = true;
    protected OnCameraListener mCameraListener;

    public Camera2Helper(OnCameraListener cameraListener) {
        mCameraListener = cameraListener;
        initImageSize();
    }

    private void initImageSize() {
        if (MotorUtil.CVT_EN_REMOTE_CONTROL_FOCUS) {
            SIZE_WIDTH = 1280;
            SIZE_HEIGHT = 720;
        } else {
            SIZE_WIDTH = 640;
            SIZE_HEIGHT = 480;
        }
    }

    private void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraThread");
            mBackgroundThread.setPriority(Thread.MAX_PRIORITY);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
        if (mProcessThread == null) {
            mProcessThread = new HandlerThread("ProcessThread");
            mProcessThread.setPriority(Thread.MAX_PRIORITY);
            mProcessThread.start();
            mProcessHandler = new Handler(mProcessThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        try {
            if (mBackgroundHandler != null) {
                mBackgroundHandler.removeCallbacksAndMessages(null);
            }

            if (mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            }


            if (mProcessHandler != null) {
                mProcessHandler.removeCallbacksAndMessages(null);
            }

            if (mProcessThread != null) {
                mProcessThread.quitSafely();
                mProcessThread.join();
                mProcessThread = null;
                mProcessHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setOpenDebug(boolean openDebug) {
        mOpenDebug = openDebug;
    }

    public void closeCamera() {

        try {
            mCameraOpenCloseLock.acquire();

            closePreviewSession();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            stopBackgroundThread();

            if (mImageReader != null) {
                mImageReader.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
        mBuffer = null;
    }


    public boolean checkIfOpenCamera() {
        return mCameraDevice != null;
    }


    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    /**
     * @param context 上下文
     */
    public void openCamera(Context context) {
        startBackgroundThread();
        if (mCameraDevice == null) {
            openCameraDevice(context);
        } else {
            if (mOpenDebug) {
                LogUtil.d("camera is already open");
            }
        }
    }

    /**
     * 获取摄像头采集的参数
     *
     * @param cameraManager 摄像头管理
     * @param cameraId      摄像头ID
     */
    private void getCameraParams(CameraManager cameraManager, String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        // 输出的画面信息
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Integer mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (mOpenDebug) {
            LogUtil.d("mSensorOrientation: " + mSensorOrientation);
        }
        if (map == null) {
            throw new RuntimeException("Cannot get available preview/video sizes");
        }


        mPreviewSize = new Size(SIZE_WIDTH, SIZE_HEIGHT);
//        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class));
        SIZE_WIDTH = mPreviewSize.getWidth();
        SIZE_HEIGHT = mPreviewSize.getHeight();
        if (mOpenDebug) {
            LogUtil.d("mPreviewSize: " + mPreviewSize);
        }


        // 是否支持闪光灯,TV上不需要
        Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        boolean mFlashSupported = flashAvailable == null ? false : flashAvailable;

        mFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (mOpenDebug) {
            LogUtil.d("mFpsRanges: " + Arrays.toString(mFpsRanges));
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices 摄像头支持的输出尺寸
     * @return 筛选出最优的尺寸（优先宽高一致的，否则选比例最接近+长宽最接近的
     */
    private Size chooseOptimalSize(Size[] choices) {
        float reqRatio = ((float) mPreviewWidth) / mPreviewHeight;
        List<Size> tempSizeList = new ArrayList<>();

        for (Size option : choices) {
            if (mOpenDebug) {
                LogUtil.d("support Optimal size: " + option);
            }
            float curRatio = ((float) option.getWidth()) / option.getHeight();
            if (curRatio == reqRatio) {
                if (option.getWidth() >= SUPPORT_WIDTH_MIN) {
                    tempSizeList.add(option);
                }
            }
        }

        if (tempSizeList.isEmpty()) {
            return choices[choices.length - 1];
        }

        Size minSize = tempSizeList.get(0);
        int minWidth = Integer.MAX_VALUE;
        for (Size size : tempSizeList) {
            if (size.getWidth() < minWidth) {
                minSize = size;
            }
        }
        return minSize;
    }

    @SuppressLint("MissingPermission")
    private void openCameraDevice(Context context) {

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            return;
        }
        try {
            LogUtil.d("tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                // 主要确保只有一个对象获取了 Camera， 2.5s 还没能释放，就会抛出异常
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            // TV 上应该只有一个摄像头，如果存在多个摄像头，还需要从 CameraIdLIst 中筛选
            final String cameraId = cameraManager.getCameraIdList()[0];
            getCameraParams(cameraManager, cameraId);
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    LogUtil.d("onOpened: " + camera);
                    mCameraDevice = camera;
                    startPreview();
                    mCameraOpenCloseLock.release();
                    mCameraListener.onCameraStarted();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    if (mCameraDevice != null) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                    mCameraOpenCloseLock.release();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    LogUtil.d("openCamera error code: " + error);
                    mCameraOpenCloseLock.release();
                    mCameraListener.onCameraError(error);

                }
            }, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            mCameraOpenCloseLock.release();
            mCameraListener.onCameraError(ErrorCode.CAMERA_OPEN_ERROR);
        }
    }


    private void startPreview() {
        closePreviewSession();
        if (mImageReader != null) {
            mImageReader.close();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        try {
            LogUtil.d("startPreview");
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            List<Surface> surfaceList = new ArrayList<>();

            ImageReader imageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight()
                    , ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(Camera2Helper.this, mBackgroundHandler);
            mImageReader = imageReader;
            final Surface bitmapSurface = imageReader.getSurface();
            mPreviewBuilder.addTarget(bitmapSurface);
            surfaceList.add(bitmapSurface);


//            SurfaceTexture surfaceTexture = new SurfaceTexture(false);
//            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            mSurfaceTexture = surfaceTexture;
//            final Surface previewSurface = new Surface(surfaceTexture);
//            mPreviewBuilder.addTarget(previewSurface);
//            surfaceList.add(previewSurface);

            mCameraDevice.createCaptureSession(surfaceList,
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            LogUtil.d("onConfigured: " + session);
                            updatePreview();
                        }

                        @Override
                        public void onActive(@NonNull CameraCaptureSession session) {
                            super.onActive(session);
                            LogUtil.d("onActive: " + session);
                            // 开始处理捕捉请求
                        }

                        @Override
                        public void onReady(@NonNull CameraCaptureSession session) {
                            super.onReady(session);
                            LogUtil.d("onReady: " + session);
                            // 暂时没有捕捉请求
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            LogUtil.d("onConfigureFailed: " + session);
                        }
                    }, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            mCameraListener.onCameraError(ErrorCode.CAMERA_START_PREVIEW_ERROR);
        }
    }

    private void updatePreview() {
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            mCameraListener.onCameraError(ErrorCode.CAMERA_SHOW_PREVIEW_ERROR);
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        if (mFpsRanges != null) {
//            Range<Integer> fpsRange = mFpsRanges[mFpsRanges.length - 1];
            Range<Integer> fpsRange = mFpsRanges[0];
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            LogUtil.d("set fps range = " + fpsRange.toString());

        }
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }


    @Override
    public void onImageAvailable(ImageReader reader) {

        final Image image = reader.acquireLatestImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return;
        }
        if (ImageFormat.YUV_420_888 == image.getFormat()) {

            final byte[] yuv = ImageUtil.YUV_420_888toNV21(image);
            // final byte[] yuv = ImageUtil.getDataFromImage(image, ImageUtil.COLOR_FormatNV21);
            image.close();

            mProcessHandler.removeCallbacksAndMessages(null);
            mProcessHandler.post(new Runnable() {
                @Override
                public void run() {
                    // bitmap
                    Bitmap bitmap = ImageUtil.nv21ToBitmap1(yuv, SIZE_WIDTH, SIZE_HEIGHT);

                    if (mCameraListener != null) {
                        mCameraListener.onCaptureComplete(bitmap, null);
                    }
                }
            });
        }
    }


    public interface OnCameraListener {
        void onCameraStarted();

        void onCameraError(int error);

        void onCaptureComplete(Bitmap bitmap, File file);
    }


}
