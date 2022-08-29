package com.cvte.autoprojector;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;

public class ShowPattern2 {
    private final WindowManager mWindowManager;
    private final ViewGroup mWindowContainer;
    private final WindowManager.LayoutParams mParams;
    private static ShowPattern2 mInstance;

    public static ShowPattern2 getInstance(Context context) {

        if (mInstance == null) {
            synchronized (ShowPattern2.class) {
                if (mInstance == null) {
                    mInstance = new ShowPattern2(context);
                }
            }
        }
        return mInstance;
    }
    public ShowPattern2(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowContainer = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.showpattern2, null);
        mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.format = PixelFormat.RGBA_8888;
    }

    public void addView2() {
        try{
            mWindowManager.addView(mWindowContainer,mParams);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public void removeView2() {
        try{
            mWindowManager.removeView(mWindowContainer);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
