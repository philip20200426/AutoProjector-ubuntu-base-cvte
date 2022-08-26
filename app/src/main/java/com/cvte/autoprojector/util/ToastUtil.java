package com.cvte.autoprojector.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.cvte.autoprojector.R;

import static android.security.KeyStore.getApplicationContext;

/**
 * Created by xiechukang on 22/07/12.
 */
public class ToastUtil {

    private static Toast mLastToast;
    private static Toast oldUIToast;
    private static Toast mToast;
    private static TextView tv;

    public static void showToast(@NonNull Context context, int msgId) {
        Builder.build(context)
                .buildIcon(R.drawable.toast_icon_warn)
                .buildText(msgId)
                .show();
    }

    public static void showToast(@NonNull Context context, String message) {
        Builder.build(context)
                .buildIcon(R.drawable.toast_icon_warn)
                .buildText(message)
                .show();
    }

    public static void showAutoProjectorToast(Context context,String str){
        if(mToast ==null){
            mToast = Toast.makeText(context, null, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.CENTER|Gravity.BOTTOM, 30, 30);
            LinearLayout toastView = (LinearLayout)mToast.getView();
            WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            tv=new TextView(getApplicationContext());
            tv.setTextSize(25);
            toastView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 60);
            tv.setLayoutParams(params);
            mToast.setView(toastView);
            toastView.addView(tv);
        }
        tv.setText(str);
        mToast.show();
    }

    /**
     *  旧UI实现
     *  @deprecated
     */
    public static void toast(int toast) {
        if (null == oldUIToast) {
            Context context = getApplicationContext();
            oldUIToast = new Toast(context);
            LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflate.inflate(R.layout.layout_toast, null);
            oldUIToast.setView(v);
            oldUIToast.setDuration(Toast.LENGTH_LONG);
        }
        oldUIToast.setText(toast);
        oldUIToast.show();
    }

    /**
     *  旧UI实现
     *  @deprecated
     */
    public static void toast(String msg, int time){
        if(oldUIToast == null){
            oldUIToast = Toast.makeText(getApplicationContext(), msg, time);
        }
        else{
            oldUIToast.setText(msg);
            oldUIToast.setDuration(time);
        }
        oldUIToast.show();
    }


    public static class Builder {

        private Toast mToast;
        private ImageView image;
        private TextView text;

        private Builder(Context context) {
            mToast = new Toast(context);
            View view = LayoutInflater.from(context).inflate(R.layout.layout_toast, null, false);
            image = view.findViewById(R.id.iv_tip);
            text = view.findViewById(R.id.tv_tip);
            mToast.setView(view);

            int yOffset = context.getResources().getDimensionPixelSize(R.dimen.p64);
            buildGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, yOffset);
            buildDuration(3000);
        }

        public static Builder build(Context context) {
            return new Builder(context);
        }

        public Builder buildIcon(@DrawableRes int icon) {
            image.setImageResource(icon);
            return this;
        }

        public Builder buildText(@StringRes int content) {
            text.setText(content);
            return this;
        }

        public Builder buildText(String content) {
            text.setText(content);
            return this;
        }

        public Builder buildDuration(int duration) {
            mToast.setDuration(duration);
            return this;
        }

        public Builder buildGravity(int gravity, int xOffset, int yOffset) {
            mToast.setGravity(gravity, xOffset, yOffset);
            return this;
        }

        public void show() {
            mToast.show();
            if (null != mLastToast) {
                mLastToast.cancel();
            }
            mLastToast = mToast;
        }
    }

}
