package com.cvte.autoprojector;

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
    private ViewGroup mWindowContainer2;

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

        try {
            mWindowManager.addView(mWindowContainer, mParams);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void removeAllView() {
        try {
            if (mWindowContainer != null && mWindowContainer.isAttachedToWindow()) {
                mWindowManager.removeView(mWindowContainer);
            }
            if (mWindowContainer2 != null && mWindowContainer2.isAttachedToWindow()) {
                mWindowManager.removeView(mWindowContainer2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void removeView() {
        try {
            if (mWindowContainer.isAttachedToWindow()) {
                mWindowManager.removeView(mWindowContainer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showPattern2(Context context) {
        mWindowContainer2 = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.showpattern2, null);
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.format = PixelFormat.RGBA_8888;
        mWindowManager.addView(mWindowContainer2, mParams);
    }


    public void hidePattern2(Context mContext) {
        try {
            if (mWindowContainer.isAttachedToWindow()) {
                mWindowManager.removeView(mWindowContainer);
            }
            if (mWindowContainer2.isAttachedToWindow()) {
                mWindowManager.removeView(mWindowContainer2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}