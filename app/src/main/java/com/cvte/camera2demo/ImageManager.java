package com.cvte.camera2demo;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.cvte.camera2demo.util.OpenCVUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片管理类
 * 1.用于存放图片信息
 * 2.最清晰图片
 */
public class ImageManager {

    private final List<ImageBean> imageList = new ArrayList<>();

    private int mDuration;
    private int maxLapsIndex = -1;

    public ImageManager() {

    }

    public void addImage(ImageBean imageBean) {
//        if (!imageList.contains(imageBean)) {
        imageList.add(imageBean);
//        }
    }

    public List<ImageBean> getImageList() {
        return imageList;
    }

    public int getImageSize() {
        return imageList.size();
    }

    public void clear() {
        imageList.clear();
        maxLapsIndex = -1;
        mDuration = 0;
    }

    public int getMaxLapsIndex() {
        return maxLapsIndex;
    }

    /**
     * 计算数据池图片清晰度
     */
    public void calculateDataPoolPhotoClarity() {
        Log.d("HBK-BC-L", "Laplace start--------------------------------");
        long timeBegin = SystemClock.uptimeMillis();
        //计算拍下来图片的清晰度的值
        for (int i = 0; i < imageList.size(); i++) {
            ImageBean imageBean = imageList.get(i);
            if (imageBean != null) {
                Bitmap bitmap = imageBean.getBitmap();
                if (bitmap != null) {
                    double laplaceValue = OpenCVUtils.calculateOneBitmapClarityWithNoGray(bitmap);
                    imageBean.setLaplacian(laplaceValue);
//                    Log.d("HBK-BC-L", "laplaceValue[" + i + "]:" + laplaceValue);
                    Log.d("HBK-BC-L", imageBean.toString());
                }
            }
        }
        Log.d("HBK-BC-L", "Laplace end--------------------------耗时：" + (SystemClock.uptimeMillis() - timeBegin));
        calculateBitmapPoolLaplaceMax();
    }

    /**
     * 查找清晰度最大的值
     */
    public void calculateBitmapPoolLaplaceMax() {
        if (imageList.isEmpty()) {
            return;
        }
        ImageBean maxLapsBitmap = imageList.get(0);
        for (int i = 0; i < imageList.size(); i++) {
            ImageBean imageBean = imageList.get(i);
            if (imageBean != null) {
                if (imageBean.getLaplacian() > maxLapsBitmap.getLaplacian()) {
                    maxLapsBitmap = imageBean;
                }
            }
        }
        maxLapsIndex = maxLapsBitmap.getIndex();
        Log.d("HBK-BC-L", "Pool MAX value:" + maxLapsBitmap.getLaplacian() + " index:" + maxLapsBitmap.getIndex());
    }

    /**
     * 计算总时间
     *
     * @return
     */
    public void calculateTotalDuration() {
        mDuration = 0;
        for (ImageBean imageBean : imageList) {
            mDuration += imageBean.getDuration();
        }
    }

    /**
     * 获取返回步数
     *
     * @param totalStep 总的步数
     * @return 电机返回的步数
     */
    public int getReturnSteps(int totalStep) {
        Log.d("HB-BC-R", "totalStep: " + totalStep);
        if (imageList.isEmpty() || maxLapsIndex >= imageList.size()) {
            return 0;
        }
        calculateTotalDuration();
        Log.d("HB-BC-R", "mDuration: " + mDuration);
        double stepRatio = (double) totalStep / (double) mDuration;//步长率
        Log.d("HB-BC-R", "stepRatio: " + stepRatio);
        int returnDuration = 0;
        for (int i = maxLapsIndex; i < imageList.size(); i++) {
            ImageBean imageBean = imageList.get(i);
            returnDuration += imageBean.getDuration();
        }
        int returnSteps = (int) (returnDuration * stepRatio);
        Log.d("HB-BC-R", "returnSteps: " + returnSteps);
        return returnSteps;
    }
}
