package com.cvte.autoprojector;

import android.graphics.Bitmap;

import java.util.Objects;

/**
 * 对焦需要的图片信息类
 */
public class LocationBean {
    private int index;
    private double laplacian;
    private int steps;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getLaplacian() {
        return laplacian;
    }

    public void setLaplacian(double laplacian) {
        this.laplacian = laplacian;
    }

    public double getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationBean imageBean = (LocationBean) o;
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
                ", steps=" + steps +
                '}';
    }
}
