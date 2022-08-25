package com.cvte.camera2demo.util;

import static com.cvte.camera2demo.util.Constants.PERSIST_BEGIN_TAKE_PHOTO;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import com.cvte.adapter.android.os.SystemPropertiesAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;


/**
 * Created by xiaqing@cvte.com
 * 2020/3/10 22:27
 * <p>
 * I420: YYYYYYYYUUVV
 * YV12: YYYYYYYYVVUU
 * NV12: YYYYYYYYUVUV
 * NV21: YYYYYYYYVUVU
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";
    private static final String mSavePath = "/sdcard/DCIM/";

    public static byte[] getI420DataFromImage(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height;
                    outputStride = 1;
                    break;
                case 2:
                    channelOffset = (int) (width * height * 1.25);
                    outputStride = 1;
                    break;
                default:
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    public static int getImageSizeForI420(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
        return width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
    }

    //VUVU => UUVV
    private static byte[] swapNV21toI420(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        int size = width * height;
        for (int i = 0; i < size / 4; i += 1) {
            i420bytes[size + i] = nv21bytes[size + 2 * i + 1];
            i420bytes[size + size / 4 + i] = nv21bytes[size + 2 * i];
        }
        return i420bytes;
    }

    public static byte[] getI420FromYUV_420_888(Image image, byte[] buffer) throws IllegalAccessException {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalAccessException();
        }
        int width = image.getWidth();
        int height = image.getHeight();


//        byte[] i420bytes = new byte[buffer.length];
////
//        YuvUtil.yuvNV21ToI420(buffer,width,height,i420bytes);
//
//        return i420bytes;
//        return swapNV21toI420(buffer, i420bytes, width, height);


        //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
        int size = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        byte[] yuv = buffer;
        if (yuv == null || yuv.length != size) {
            yuv = new byte[size];
        }
        final Image.Plane[] planes = image.getPlanes();

        // 处理 Y
        ByteBuffer bufferY = planes[0].getBuffer();
        int remainingY = bufferY.remaining();
        bufferY.get(yuv, 0, remainingY);

        // 处理 UV
        ByteBuffer bufferU = planes[1].getBuffer();
        ByteBuffer bufferV = planes[2].getBuffer();
        int remainingU = bufferU.remaining();
        int remainingV = bufferV.remaining();

        if (planes[1].getPixelStride() == 1) {
            bufferU.get(yuv, remainingY, remainingU);
            bufferV.get(yuv, remainingY + remainingU, remainingV);
        } else {
            byte[] uVBytes = new byte[remainingU];
            bufferU.get(uVBytes);
            final int realUVLength = (int) (width * height * 0.25);
            int srcIndex = 0;
            for (int i = 0; (srcIndex < realUVLength && i < uVBytes.length); i += 2, srcIndex++) {
                yuv[remainingY + srcIndex] = uVBytes[i];
            }
            bufferV.get(uVBytes);
            srcIndex = 0;
            for (int i = 0; (srcIndex < realUVLength && i < uVBytes.length); i += 2, srcIndex++) {
                yuv[remainingY + realUVLength + srcIndex] = uVBytes[i];
            }
        }
        return yuv;
    }


    private static boolean mOpenDebug = false;
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;


    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
//        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
//            if (VERBOSE) {
//                Log.v(TAG, "pixelStride " + pixelStride);
//                Log.v(TAG, "rowStride " + rowStride);
//                Log.v(TAG, "width " + width);
//                Log.v(TAG, "height " + height);
//                Log.v(TAG, "buffer size " + buffer.remaining());
//            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
//            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }


    private static byte[] nv21;
    private static int ySize;
    private static int uvSize;
    private static int yuvWidth;
    private static int yuvHeight;

    public static byte[] YUV_420_888toNV21(Image image) {


        if (nv21 == null) {
            yuvWidth = image.getWidth();
            yuvHeight = image.getHeight();
            ySize = yuvWidth * yuvHeight;
            uvSize = yuvWidth * yuvHeight / 4;
            nv21 = new byte[ySize + uvSize * 2];
        }

//        int yuvWidth = image.getWidth();
//        int yuvHeight = image.getHeight();
//        int ySize = yuvWidth * yuvHeight;
//        int uvSize = yuvWidth * yuvHeight / 4;
//        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;
        if (rowStride == yuvWidth) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            int yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += yuvWidth) {
                yBufferPos += rowStride;
                yBuffer.position(yBufferPos);
                yBuffer.get(nv21, pos, yuvWidth);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == yuvWidth && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < yuvHeight / 2; row++) {
            for (int col = 0; col < yuvWidth / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }


    public static Bitmap nv12ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 0, 1);
    }

    public static Bitmap nv21ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 1, 0);
    }

    private static Bitmap spToBitmap(byte[] data, int w, int h, int uOff, int vOff) {
        int plane = w * h;
        int[] colors = new int[plane];
        int yPos = 0, uvPos = plane;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uvPos + uOff] & 0xff) - 128;
                final int v = (data[uvPos + vOff] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if ((yPos++ & 1) == 1) uvPos += 2;
            }
            if ((j & 1) == 0) uvPos -= w;
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    public static Bitmap nv21ToBitmap1(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    public static Bitmap i420ToBitmap(byte[] data, int w, int h) {
        return pToBitmap(data, w, h, true);
    }

    public static Bitmap yv12ToBitmap(byte[] data, int w, int h) {
        return pToBitmap(data, w, h, false);
    }

    private static Bitmap pToBitmap(byte[] data, int w, int h, boolean uv) {
        int plane = w * h;
        int[] colors = new int[plane];
        int off = plane >> 2;
        int yPos = 0, uPos = plane + (uv ? 0 : off), vPos = plane + (uv ? off : 0);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uPos] & 0xff) - 128;
                final int v = (data[vPos] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if ((yPos++ & 1) == 1) {
                    uPos++;
                    vPos++;
                }
            }
            if ((j & 1) == 0) {
                uPos -= (w >> 1);
                vPos -= (w >> 1);
            }
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }


    public static int width = 0;
    public static int height = 0;

    /**
     * Save Bitmap
     */
    public static void saveBitmap(String name, Bitmap bitmap) {
        LogUtil.d("Ready to save picture" + name);
        //指定我们想要存储文件的地址
        //判断指定文件夹的路径是否存在
        File file = new File(mSavePath);
        if (!file.exists()) {
            LogUtil.d("mSavePath isn't exist");
            file.mkdir();
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(mSavePath, name);
            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                LogUtil.d("Save Path:" + saveFile.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Save Bitmap
     */
    public static void saveBitmap1(String name, Bitmap bitmap) {
        //判断指定文件夹的路径是否存在
        File file = new File(mSavePath);
        if (!file.exists()) {
            LogUtil.d(mSavePath + " isn't exist");
            file.mkdir();
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(mSavePath, name);
            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                LogUtil.d("filePath:" + saveFile.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Save Bitmap
     */
    public static void saveBlankBitmap(Bitmap bitmap) {
        String path = "/sdcard/Pictures/";
        //判断指定文件夹的路径是否存在
        File file = new File(path);
        if (!file.exists()) {
            LogUtil.d(path + " isn't exist");
            file.mkdir();
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(path, "white_tmp.png");
            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                LogUtil.d(" filePath:" + saveFile.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * 将Bitmap存为 .bmp格式图片
     *
     * @param bitmap
     */
    public static void saveBmp(String name, Bitmap bitmap) {
        if (bitmap == null)
            return;
        // 位图大小
        int nBmpWidth = bitmap.getWidth();
        int nBmpHeight = bitmap.getHeight();
        // 图像数据大小
        int bufferSize = nBmpHeight * (nBmpWidth * 3 + nBmpWidth % 4);
        try {
            // 存储文件名
            Log.d("HBK", "Ready to save picture");
            //指定我们想要存储文件的地址
            Log.d("HBK", "Save Path=" + mSavePath);

            File fileDir = new File(mSavePath);
            if (!fileDir.exists()) {
                Log.d("HBK", "TargetPath isn't exist");
                fileDir.mkdir();
            } else {
                String filename = mSavePath + name;
                Log.d("HBK", "filename = " + filename);
                File file = new File(filename);
                if (!file.exists()) {
                    Log.d("HBK", "filename isn't exist");
                    file.createNewFile();
                }
                FileOutputStream fileos = new FileOutputStream(filename);
                // bmp文件头
                int bfType = 0x4d42;
                long bfSize = 14 + 40 + bufferSize;
                int bfReserved1 = 0;
                int bfReserved2 = 0;
                long bfOffBits = 14 + 40;
                // 保存bmp文件头
                writeWord(fileos, bfType);
                writeDword(fileos, bfSize);
                writeWord(fileos, bfReserved1);
                writeWord(fileos, bfReserved2);
                writeDword(fileos, bfOffBits);
                // bmp信息头
                long biSize = 40L;
                long biWidth = nBmpWidth;
                long biHeight = nBmpHeight;
                int biPlanes = 1;
                int biBitCount = 24;
                long biCompression = 0L;
                long biSizeImage = 0L;
                long biXpelsPerMeter = 0L;
                long biYPelsPerMeter = 0L;
                long biClrUsed = 0L;
                long biClrImportant = 0L;
                // 保存bmp信息头
                writeDword(fileos, biSize);
                writeLong(fileos, biWidth);
                writeLong(fileos, biHeight);
                writeWord(fileos, biPlanes);
                writeWord(fileos, biBitCount);
                writeDword(fileos, biCompression);
                writeDword(fileos, biSizeImage);
                writeLong(fileos, biXpelsPerMeter);
                writeLong(fileos, biYPelsPerMeter);
                writeDword(fileos, biClrUsed);
                writeDword(fileos, biClrImportant);
                // 像素扫描
                byte bmpData[] = new byte[bufferSize];
                int wWidth = (nBmpWidth * 3 + nBmpWidth % 4);
                for (int nCol = 0, nRealCol = nBmpHeight - 1; nCol < nBmpHeight; ++nCol, --nRealCol)
                    for (int wRow = 0, wByteIdex = 0; wRow < nBmpWidth; wRow++, wByteIdex += 3) {
                        int clr = bitmap.getPixel(wRow, nCol);
                        bmpData[nRealCol * wWidth + wByteIdex] = (byte) Color.blue(clr);
                        bmpData[nRealCol * wWidth + wByteIdex + 1] = (byte) Color.green(clr);
                        bmpData[nRealCol * wWidth + wByteIdex + 2] = (byte) Color.red(clr);
                    }

                fileos.write(bmpData);
                fileos.flush();
                fileos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getmSavePath() {
        return mSavePath;
    }

    protected static void writeWord(FileOutputStream stream, int value) throws IOException {
        byte[] b = new byte[2];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        stream.write(b);
    }

    protected static void writeDword(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    protected static void writeLong(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    public static double[] laplaceValue = new double[1024];
    public static int laplaceCounter;
    public static double laplaceBiggestValue;
    public static int laplaceBiggestCount;
    public static double laplace2thBiggestValue = 0;
    public static int laplace2thBiggestCount = 0;
    public static double laplaceBiggestValueCheck = 0;
    public static int laplaceBiggestCountCheck = 0;
    public static double laplaceMinimumValue = 0;
    public static int laplaceMinimumCount = 0;
    public static int laplaceMaxCountCheck = 0;
    public static int laplaceMaxCount = 0;

    public static void cleanLaplaceValue() {
        for (int i = 0; i < laplaceCounter; i++) {
            laplaceValue[i] = 0;
        }
    }

    public static boolean AutoFocusFinishedToKeystone = false;
    public static boolean KeystonePositiveFinishedToNegative = false;
    public static String KeystoneBmp = "pattern_01.bmp";

    //标准差计算
    public static double laplaceGapStandardDeviation = 0;
    public static double laplaceGapValueSum = 0;

    /*
     * 计算步数 & 回转的算法 start
     *  */
    //图片数据池
    public static int BITMAP_MAX_COUNT = 256;
    public static Bitmap[] bitmapPool = new Bitmap[BITMAP_MAX_COUNT];

    public static void cleanBitmapPool() {
        for (int i = 0; i < BITMAP_MAX_COUNT; i++) {
            Bitmap bitmap = bitmapPool[i];
            if (bitmap != null) {
                bitmap.recycle();
                bitmapPool[i] = null;
            }
        }
    }

    /**
     * 删除照片
     *
     * @param prefix 照片前缀
     */
    public static void removeLocalImages(String prefix) {
        File file = new File(ImageUtil.getmSavePath());
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith(prefix)) {
                        f.delete();
                    }
                }
            }
        }
    }

    /**
     * 记录拍照 拍照图片大小
     */
    public static int bitmapPoolLength;
    /**
     * 位图清晰度最高图片索引
     */
    public static int bitmapPoolBiggestIndex;
    /**
     * 位图清晰度最大值
     */
    public static double bitmapPoolBiggestValue;

    public static double lastBitmapPoolBiggestValue;
    /**
     * 位图池最大计数检查
     */
    public static int bitmapPoolBiggestCountCheck;
    /**
     * 位图池最大值检查
     */
    public static double bitmapPoolBiggestValueCheck;
    /**
     * bitmapPoolMaxCountCheck
     */
    public static int bitmapPoolMaxCountCheck = 0;

    public static void initBitmapPool() {
        bitmapPoolLength = 0;
        bitmapPoolBiggestIndex = 0;
        bitmapPoolBiggestValue = 0.0;
        lastBitmapPoolBiggestValue = 0.0;
        bitmapPoolBiggestCountCheck = 0;
        bitmapPoolBiggestValueCheck = 0.0;
        bitmapPoolMaxCountCheck = 0;
        cleanBitmapPool();
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
    }

    public static void resetBitmapPool() {
        bitmapPoolLength = 0;
        bitmapPoolBiggestIndex = 0;
        bitmapPoolBiggestValue = 0.0;
        bitmapPoolBiggestCountCheck = 0;
        bitmapPoolBiggestValueCheck = 0.0;
        bitmapPoolMaxCountCheck = 0;
        cleanBitmapPool();
        SystemPropertiesAdapter.set(PERSIST_BEGIN_TAKE_PHOTO, "0");
    }

}
