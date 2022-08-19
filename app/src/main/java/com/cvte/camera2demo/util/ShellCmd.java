package com.cvte.camera2demo.util;

import com.cvte.at.platform.AtShellCmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 窗口命令行执行工具
 */
public class ShellCmd {

    /**
     * 窗口命令行执行工具
     *
     * @param cmd 串口命令
     */
    public static boolean exec(String cmd) {
        boolean result = false;
        Process process;
        try {
            process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream()));
            String data = "";
            while ((data = reader.readLine()) != null) {
                System.out.println(data);
            }

            int exitValue = process.waitFor();

            if (exitValue != 0) {
                System.out.println("error");
            }
            result = exitValue == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
