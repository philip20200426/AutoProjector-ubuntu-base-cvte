package com.cvte.autoprojector.util;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

public class Constants {
    //projector Data Save to System
    public static final String FILE_SAVE_PATH = "/data/trapezoid";
    public static final String FILE_SAVE_NAME = "/data/trapezoid/trapezoidcorrect_points_offset.ini";
    public static final String FILE_SAVE_DATA_TITLE_STR = "[trapezoidpointoffset]";
    public static final String FILE_SAVE_DATA_LT_STR = "lt";
    public static final String FILE_SAVE_DATA_LB_STR = "lb";
    public static final String FILE_SAVE_DATA_RT_STR = "rt";
    public static final String FILE_SAVE_DATA_RB_STR = "rb";
    public static final String FILE_SAVE_DATA_TYPE_STR = "type";
    public static final String FILE_SAVE_DATA_TYPE_STATUS = "0x00000001";
    public static final String FILE_SAVE_DATA_RESET_NUM = "0x00000000:0x00000000";
    public static final String NUM_LOCATION_DEF_VALUE = "0:0";


    // MTK common KEYSTONE FOR HDCP
    public static final boolean CVT_EN_KEYSTONE_FOR_HDCP = SystemPropertiesAdapter.get("ro.CVT_EN_KEYSTONE_FOR_HDCP", "0").equals("1");

    /**
     * <ul>
     *     <li>0:默认状态</li>
     *     <li>1:拍照状态</li>
     *     <li>2:停止拍照</li>
     *     <li>999:相机异常</li>
     * </ul>
     */
    public static final String PERSIST_BEGIN_TAKE_PHOTO = "persist.begin.take.photo";
    public static final String PERSIST_FINISH_TAKE_PHOTO = "persist.finish.take.photo";
}
