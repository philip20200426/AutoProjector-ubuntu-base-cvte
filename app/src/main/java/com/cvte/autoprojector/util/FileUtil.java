package com.cvte.autoprojector.util;

import android.util.Log;

import com.cvte.autoprojector.ImageManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileUtil {


    public static int mFileLines;
    /**
     * 导出
     * <p>
     * file csv文件(路径+文件名)，csv文件不存在会自动创建
     * dataList 数据
     *
     * @return
     */
    public static void exportCsv(String path, ImageManager imageManager) {
        BufferedWriter bfw;
        File file = new File(path);
        FileOutputStream out = null;
        OutputStreamWriter osw = null;
        FileInputStream in;
        Scanner scanner;
        boolean hasDir = file.exists();
        try {
            out = new FileOutputStream(file, true);
            //用Excel打开，中文会乱码，所以用GBK编译。
            osw = new OutputStreamWriter(out, "GBK");
            bfw = new BufferedWriter(osw);
            in = new FileInputStream(file);
            if (!hasDir) {
                //第一行表头数据
                //bfw.write("Number" + ',');
                //bfw.write("" + ',');
                //bfw.write("Laplace");
                // 写好表头后换行
                bfw.write("Laplace0" + ",");
                Log.d("philip", " Create files : " + path);
            } else {
                bfw.newLine();
                readFileLines(path);
                Log.d("philip", path + " have exited " + "mFileLines : " + mFileLines);
                bfw.write("Laplace" + mFileLines + ",");
            }

            //表格数据
            for (int i = 0; i < imageManager.getImageSize(); i++) {
                //bfw.write(String.valueOf(i) + ',');
                //String sourceString = scanner.nextLine();
                //bfw.write(sourceString + ",");
                //bfw.write(String.valueOf(imageManager.getImageList().get(i).getFrameId()) + ',');
                bfw.append(String.valueOf(imageManager.getImageList().get(i).getLaplacian()) + ',');
                //bfw.newLine();
            }
            // 将缓存数据写入文件
            bfw.flush();
            // 释放缓存
            bfw.close();
            osw.close();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //读取CSV文件
    public static void readCSV(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileInputStream fiStream;
        Scanner scanner;
        try {
            fiStream = new FileInputStream(file);
            //读取的中文乱码，用GBK编译。
            scanner = new Scanner(fiStream, "GBK");
            //scanner.nextLine();//读下一行,把表头越过。
            while (scanner.hasNextLine()) {
                //读取整行数据
                String sourceString = scanner.nextLine();
                Log.d("temporary", "csv data: " + sourceString + "length : " + sourceString.length());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

public static void readFileLines(String file) {
        try {
            //创建字符缓冲输入流对象，指定源文件路径
            BufferedReader br = new BufferedReader(new FileReader(file));
            //当读取一行后java代码行数+1
            while (br.readLine()!=null) {
                mFileLines++;
            }
        } catch (Exception e) {
            //若是程序出现异常则进行捕获并在控制台打印异常信息
            System.out.println("程序出现异常,异常信息:"+e.getMessage());
        }
}

    /*    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }*/




/*    public class Demo {
        //因为要再静态方法中使用该变量,所以定义为静态，此变量用来存储java代码行数
        static long count;
        public static void main(String[] args) {
            //调用getSum方法指定需要查询的代码在本地存储的位置,这里放的是绝对路径
            System.out.println("写的代码行数:"+getSum(new File("D:\\myself\\study\\code")));
        }
        public static long getSum(File f) {
            //获得当前路径下的所有文件夹
            File[] arr = f.listFiles();
            //判断当前路径不为空
            if (arr!=null) {
                //变量当前路径的所有文件
                for (File file : arr) {
                    //如果类型属于文件并且文件的后缀名为“java”
                    if (file.isFile() && file.getName().endsWith("java")) {
                        try {
                            //创建字符缓冲输入流对象，指定源文件路径
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            //当读取一行后java代码行数+1
                            while (br.readLine()!=null) {
                                count++;
                            }
                        } catch (Exception e) {
                            //若是程序出现异常则进行捕获并在控制台打印异常信息
                            System.out.println("程序出现异常,异常信息:"+e.getMessage());
                        }
                    }
                    //如果当前是文件夹则再次调用getSum方法进行判断,直到读完指定源文件路径下的所有文件夹及其所有子文件
                    if (file.isDirectory()) {
                        getSum(file);
                    }
                }
            }
            //将最终的结果返回
            return count;
        }
    }*/

}
