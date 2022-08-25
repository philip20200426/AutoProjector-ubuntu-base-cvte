package com.cvte.camera2demo.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.cvte.camera2demo.R;

public class PatternManager {
    WindowManager mWindowManager;
    ViewGroup mWindowContainer;
    ViewGroup mWindowContainer2;

    public PatternManager(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowContainer = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.showpattern, null);
        mWindowContainer2 = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.showpattern2, null);
    }


    public void showPattern() {
        removeAllView();
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.format = PixelFormat.RGBA_8888;
        mWindowManager.addView(mWindowContainer, mParams);
    }

    public void showPattern2() {
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.format = PixelFormat.RGBA_8888;
        mWindowManager.addView(mWindowContainer2, mParams);
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
}
