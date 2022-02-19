package com.cvte.camera2demo.util;

import android.util.Log;



public class LogUtil {
    private static final String TAG = LogUtil.class.getSimpleName();
    static boolean isOpenDebug = true;

    public static void v(String msg) {
        if (isOpenDebug) {
            Log.v(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (isOpenDebug) {
            Log.i(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (isOpenDebug) {
            Log.d(TAG, msg);
        }
    }


    public static void e(String msg) {
        Log.e(TAG, msg);
    }


}
