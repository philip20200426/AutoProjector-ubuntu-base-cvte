package com.cvte.autoprojector;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.cvte.autoprojector.util.OpenCVUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片管理类
 * 1.用于存放图片信息
 * 2.最清晰图片
 */
public class ImageManager {

    private final List<ImageBean> imageList = new ArrayList<>();
    private final List<LocationBean> locationList = new ArrayList<>();

    private int mDuration;
    private int maxLapsIndex = -1;
    private int mLocationCount = 0;
    private int mLocationIndex = 0;
    private double laplacian = 0.0;
    private int mSteps;

    private int mImageCount = 0;

    public void decreaseImageCount() {
        this.mImageCount--;
        //Log.d("philip", "mImageCount " + mImageCount);
    }
    public void increaseImageCount() {
        this.mImageCount++;
        //Log.d("philip", "mImageCount " + mImageCount);
    }
    public int getImageCount() {
        return mImageCount;
    }

    public ImageManager() {
        mImageCount = 0;
    }

    public List<LocationBean> getLocationList() {
        return locationList;
    }

    public int getSteps() {
        return mSteps;
    }

    public void setSteps(int steps) {
        this.mSteps = steps;
    }

    public int getLocationIndex() {
        return mLocationIndex;
    }

    public void setLocationIndex(int mLocationIndex) {
        this.mLocationIndex = mLocationIndex;
    }

    public void setmLocation(int mLocationCount) {
        this.mLocationCount = mLocationCount;
    }

    public int getmLocation() {
        return mLocationCount;
    }

    public void addImage(ImageBean imageBean) {
//        if (!imageList.contains(imageBean)) {
        imageList.add(imageBean);
//        }
    }
    public void addLocation(LocationBean locationBean) {
//        if (!imageList.contains(imageBean)) {
        locationList.add(locationBean);
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
        //calculateBitmapPoolLaplaceMax();
        calculateMultiLaplace();
    }
    /**
     * 计算数据池图片清晰度
     */
    public double calculateOnePhotoClarity(Bitmap bitmap) {
        if (bitmap == null) {
            return -1;
        }
        double laplaceValue = OpenCVUtils.calculateOneBitmapClarityWithNoGray(bitmap);
        return laplaceValue;
    }
    /**
     * 计算数据池图片清晰度
     */
    public double calculateMultiPhotoClarity() {
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
        return calculateBitmapPoolLaplaceMax();
        //return calculateMultiLaplace();
    }
    /**
     * 查找清晰度最大的值
     */
    public int calculateBitmapPoolLaplaceMax() {
        if (imageList.isEmpty()) {
            return -1;
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
        return maxLapsIndex;
    }

    public double calculateMultiLaplace() {
        if (imageList.isEmpty()) {
            Log.d("philip", "imageList is empty ERROR ERROR ERROR ERROR ERROR");
            return -1;
        }
        double sumLa = 0;
        double curentLa = 0;
        double averageLa = 0;
        ImageBean maxLapsBitmap = imageList.get(0);
        ImageBean minLapsBitmap = imageList.get(0);
        if (imageList.size() > 2) {
            for (int i = 0; i < imageList.size(); i++) {
                ImageBean imageBean = imageList.get(i);
                curentLa = imageBean.getLaplacian();
                if (imageBean != null) {
                    if (curentLa > maxLapsBitmap.getLaplacian()) {
                        maxLapsBitmap = imageBean;
                    }
                    if (curentLa < minLapsBitmap.getLaplacian()) {
                        minLapsBitmap = imageBean;
                    }
                    sumLa += curentLa;
                }
            }
            averageLa = (sumLa - maxLapsBitmap.getLaplacian() -minLapsBitmap.getLaplacian())/(imageList.size()-2);
            //Log.d("HBK-BC-L", "philip Pool MAX value:" + maxLapsBitmap.getLaplacian() + " index:" + maxLapsBitmap.getIndex());
            //Log.d("HBK-BC-L", "philip Pool Min value:" + minLapsBitmap.getLaplacian() + " index:" + minLapsBitmap.getIndex());
        } else if (imageList.size() == 2) {
            for (int i = 0; i < imageList.size(); i++) {
                curentLa = imageList.get(i).getLaplacian();
                sumLa += curentLa;
            }
            averageLa = sumLa/2;
        } else if (imageList.size() == 1) {
            averageLa = imageList.get(0).getLaplacian();
        }
        //double averageLa = (sumLa - maxLapsBitmap.getLaplacian() -minLapsBitmap.getLaplacian())/(imageList.size()-2);
        Log.d("philip", "averageLa : "+ averageLa + "image length: " + imageList.size());
        return averageLa;
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
