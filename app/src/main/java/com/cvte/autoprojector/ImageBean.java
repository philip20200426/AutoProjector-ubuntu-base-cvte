package com.cvte.autoprojector;

import android.graphics.Bitmap;

import java.util.Objects;

/**
 * 对焦需要的图片信息类
 */
public class ImageBean {
    private int index;
    private Bitmap bitmap;
    private double laplacian;
    private long duration;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public double getLaplacian() {
        return laplacian;
    }

    public void setLaplacian(double laplacian) {
        this.laplacian = laplacian;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageBean imageBean = (ImageBean) o;
        return index == imageBean.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    @Override
    public String toString() {
        return "ImageBean{" +
                "index=" + index +
                ", laplacian=" + laplacian +
                ", duration=" + duration +
                '}';
    }
}
