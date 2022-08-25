package com.cvte.camera2demo.util;

import static org.opencv.core.Core.mean;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenCVUtils {
    /**
     * 计算一个没有灰色的位图清晰度
     *
     * @param srcBitmap 图片
     * @return
     */
    public static double calculateOneBitmapClarityWithNoGray(Bitmap srcBitmap) {
        int kernel_size = 3;
        int ddepth = CvType.CV_8U;
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

    private static ExecutorService executor;

    public static void saveBlankPattern(Bitmap srcBitmap) {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(2);
        }
        executor.execute(() -> {
            Mat mat = new Mat();
            Utils.bitmapToMat(srcBitmap, mat);
            Log.d("HBK-BC", "保存white_tmp------------------start");
            String fileName = "/sdcard/Pictures/white_tmp.png";
            Imgcodecs.imwrite(fileName, mat);
            Log.d("HBK-BC", "保存white_tmp--------------------end");
        });
    }
}
