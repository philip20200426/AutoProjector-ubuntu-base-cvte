package com.cvte.autoprojector.util;

import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SaveKeystoneUtil {

    private static final String PROP_UI_KEYSTONE_LT = "vendor.mstar.test.pos_lt_offset";
    private static final String PROP_UI_KEYSTONE_LB = "vendor.mstar.test.pos_lb_offset";
    private static final String PROP_UI_KEYSTONE_RT = "vendor.mstar.test.pos_rt_offset";
    private static final String PROP_UI_KEYSTONE_RB = "vendor.mstar.test.pos_rb_offset";

    private static final int X_COORDINATE_SUBSCRIPT = 0;
    private static final int Y_COORDINATE_SUBSCRIPT = 1;


    /*****************************************
     * function：【投影保存】保存数据到系统
     * parameter: 无
     * return: 无
     *****************************************/
    public static void saveUserDataToSystem() {
        checkProjectorDataFile();
        clearProjectorDataFile();
        String content = updateUserProjectorData();
        writeProjectorDataFile(content);
    }

    /*****************************************
     * function：【投影保存】保存数据防呆检测
     * parameter: 无
     * return: 无
     *****************************************/
    private static void checkProjectorDataFile() {
        boolean result = true;
        result |=checkProjectorDir();
        result |=checkProjectorFile();
        Log.i("HBK-SaveKeystoneUtil","checkProjectorDataFile result is " + result);
    }

    /*****************************************
     * function：【投影保存】检查投影存储的文件夹
     * parameter: 无
     * return: 无
     *****************************************/
    private static boolean checkProjectorDir(){
        boolean result = true;
        File fileDir = new File(Constants.FILE_SAVE_PATH);
        if (!fileDir.exists()) {
            result = fileDir.mkdirs();
            Log.w("HBK-SaveKeystoneUtil","The path " + Constants.FILE_SAVE_PATH + " is " + result);
        } else {
            Log.i("HBK-SaveKeystoneUtil","The file " + Constants.FILE_SAVE_PATH + " exist !");
        }
        return result;
    }

    /*****************************************
     * function：【投影保存】检查投影存储的配置文件
     * parameter: 无
     * return: 无
     *****************************************/
    private static boolean checkProjectorFile(){
        boolean result = true;
        File file = new File(Constants.FILE_SAVE_NAME);
        if (!file.exists()) {
            try {
                result = file.createNewFile();
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            }
            Log.w("HBK-SaveKeystoneUtil","The file " + Constants.FILE_SAVE_NAME + " has been created !");
        } else {
            Log.i("HBK-SaveKeystoneUtil","The file " + Constants.FILE_SAVE_NAME + " exist !");
        }
        return result;
    }

    /*****************************************
     * function：【投影保存】清除投影存储的文件旧信息
     * parameter: 无
     * return: 无
     *****************************************/
    private static void clearProjectorDataFile() {
        File file = new File(Constants.FILE_SAVE_NAME);
        FileWriter fileWriter = null;
        try {
            if(!file.exists()) {
                file.createNewFile();
            }
            fileWriter =new FileWriter(file);

            fileWriter.write("");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    fileWriter = null;
                }
            }
        }
    }

    /*****************************************
     * function：【投影保存】更新投影存储的文件位置信息
     * parameter: 无
     * return: 对应属性设置为位置信息格式
     *****************************************/
    private static String updateUserProjectorData(){

        String ui_str_lt = SystemPropertiesAdapter.get(PROP_UI_KEYSTONE_LT, "");
        String ui_str_lb = SystemPropertiesAdapter.get(PROP_UI_KEYSTONE_LB, "");
        String ui_str_rt = SystemPropertiesAdapter.get(PROP_UI_KEYSTONE_RT, "");
        String ui_str_rb = SystemPropertiesAdapter.get(PROP_UI_KEYSTONE_RB, "");

        // MTK表示无校正会导致HDCP加密内容无法显示，加上坐标偏移patch
        if(Constants.CVT_EN_KEYSTONE_FOR_HDCP) {
            String KEYSTONE_FOR_HDCP_VALUE = "0:1";
            if(ui_str_lt.equals("0:0")) {
                SystemPropertiesAdapter.set(PROP_UI_KEYSTONE_LT, KEYSTONE_FOR_HDCP_VALUE);  // 设置为KEYSTONE_FOR_HDCP修正的值
                ui_str_lt = SystemPropertiesAdapter.get(PROP_UI_KEYSTONE_LT, "");  //重新获取KEYSTONE_FOR_HDCP下的数值
            }
        }

        String[] ui_as_lt = ui_str_lt.split(":");
        String[] ui_as_lb = ui_str_lb.split(":");
        String[] ui_as_rt = ui_str_rt.split(":");
        String[] ui_as_rb = ui_str_rb.split(":");

        BufferedReader br = null;
        StringBuffer bufAll = new StringBuffer();
        try {
            br = new BufferedReader(new FileReader(Constants.FILE_SAVE_NAME));
            StringBuffer buf = new StringBuffer();
            buf.append(Constants.FILE_SAVE_DATA_TITLE_STR).append(System.getProperty("line.separator"));
            buf.append(Constants.FILE_SAVE_DATA_LT_STR).append(" = 0x").append(Integer.toHexString(Integer.parseInt(ui_as_lt[X_COORDINATE_SUBSCRIPT])));
            buf.append(":0x").append(Integer.toHexString(Integer.parseInt(ui_as_lt[Y_COORDINATE_SUBSCRIPT]))).append(System.getProperty("line.separator"));

            buf.append(Constants.FILE_SAVE_DATA_LB_STR).append(" = 0x").append(Integer.toHexString(Integer.parseInt(ui_as_lb[X_COORDINATE_SUBSCRIPT])));
            buf.append(":0x").append(Integer.toHexString(Integer.parseInt(ui_as_lb[Y_COORDINATE_SUBSCRIPT]))).append(System.getProperty("line.separator"));

            buf.append(Constants.FILE_SAVE_DATA_RT_STR).append(" = 0x").append(Integer.toHexString(Integer.parseInt(ui_as_rt[X_COORDINATE_SUBSCRIPT])));
            buf.append(":0x").append(Integer.toHexString(Integer.parseInt(ui_as_rt[Y_COORDINATE_SUBSCRIPT]))).append(System.getProperty("line.separator"));

            buf.append(Constants.FILE_SAVE_DATA_RB_STR).append(" = 0x").append(Integer.toHexString(Integer.parseInt(ui_as_rb[X_COORDINATE_SUBSCRIPT])));
            buf.append(":0x").append(Integer.toHexString(Integer.parseInt(ui_as_rb[Y_COORDINATE_SUBSCRIPT]))).append(System.getProperty("line.separator"));
            buf.append(Constants.FILE_SAVE_DATA_TYPE_STR).append(" = ").append(Constants.FILE_SAVE_DATA_TYPE_STATUS);

            bufAll.append(buf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    br = null;
                }
            }
        }
        return bufAll.toString();
    }

    /*****************************************
     * function：【投影保存】写到投影存储信息的文件
     * parameter: 要写到的内容
     * return: 无
     *****************************************/
    private static void writeProjectorDataFile(String content){
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(Constants.FILE_SAVE_NAME));
            bw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    bw = null;
                }
            }
        }
    }
}
