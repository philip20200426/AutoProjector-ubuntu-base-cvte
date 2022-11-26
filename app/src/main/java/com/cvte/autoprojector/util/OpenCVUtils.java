package com.cvte.autoprojector.util;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.convertScaleAbs;
import static org.opencv.core.Core.mean;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
        mat = ImageUtil.cutImgROI(mat);
        Mat resizeMat = new Mat();
        //Imgproc.resize(mat, resizeMat, new org.opencv.core.Size(128, 128));

        //for test
        //ImageUtil.matToPng(mat);

        Mat lapMat = new Mat();
        Mat gauMat = new Mat();

//==================生成卷积核======================
/*        Mat kx = new Mat(3, 3, CvType.CV_8U);
        byte[] robert_x = new byte[]{0, -1, 0, -1, 5, -1, 0, -1, 0};
        kx.put(0, 0, robert_x);
        //gauMat.create(mat.size(), mat.type());
        Imgproc.filter2D(mat, gauMat, mat.depth(), kx);*/
//===================================================
        //Imgproc.GaussianBlur(mat, gauMat, new Size(3.0, 1.0), 1.0, 0, BORDER_DEFAULT);
        //Imgproc.blur(mat, gauMat, new Size(5.0, 5.0));
        //Imgproc.medianBlur(resizeMat, gauMat, 1);

        Imgproc.Laplacian(mat, lapMat, ddepth, kernel_size);
        //Core.convertScaleAbs(lapMat, lapMat);
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
        int startRow = 48, endRow = 48 + 416;
        int startCol = 384, endCol = 384 + 512;
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
