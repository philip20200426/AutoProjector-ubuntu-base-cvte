package com.cvte.camera2demo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;

public class ShowPattern {
    private final WindowManager mWindowManager;
    private final ViewGroup mWindowContainer;
    private final WindowManager.LayoutParams mParams;
    private static ShowPattern mInstance;

    public static ShowPattern getInstance(Context context) {

        if (mInstance == null) {
            synchronized (ShowPattern.class) {
                if (mInstance == null) {
                    mInstance = new ShowPattern(context);
                }
            }
        }
        return mInstance;
    }
    public ShowPattern(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowContainer = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.showpattern, null);
        mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.format = PixelFormat.RGBA_8888;
    }

    public void addView() {

        try{
            mWindowManager.addView(mWindowContainer,mParams);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public void removeView() {
        try{
            mWindowManager.removeView(mWindowContainer);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}