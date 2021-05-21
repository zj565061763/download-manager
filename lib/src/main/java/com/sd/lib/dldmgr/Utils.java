package com.sd.lib.dldmgr;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Utils {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static void postMainThread(Runnable runnable) {
        HANDLER.post(runnable);
    }

    public static File getCacheDir(String dirName, Context context) {
        File dir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dir = new File(context.getExternalCacheDir(), dirName);
        } else {
            dir = new File(context.getCacheDir(), dirName);
        }
        return dir;
    }

    public static boolean checkDir(File dir) {
        if (dir == null)
            return false;

        if (dir.exists())
            return true;

        try {
            return dir.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String MD5(String value) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(value.getBytes());
            final byte[] bytes = messageDigest.digest();

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                final String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1)
                    sb.append('0');

                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getExt(String url) {
        String ext = null;
        try {
            ext = MimeTypeMap.getFileExtensionFromUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ext == null)
            ext = "";

        return ext;
    }

    /**
     * 删除文件或者目录
     *
     * @param file
     * @return
     */
    public static boolean delete(File file) {
        if (file == null || !file.exists())
            return true;

        if (file.isFile())
            return file.delete();

        final File[] files = file.listFiles();
        if (files != null) {
            for (File item : files) {
                delete(item);
            }
        }
        return file.delete();
    }

    /**
     * 拷贝文件
     *
     * @param fileFrom
     * @param fileTo
     * @return
     */
    public static boolean copyFile(File fileFrom, File fileTo) {
        if (!checkFile(fileFrom, fileTo))
            return false;

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputStream = new FileInputStream(fileFrom);
            outputStream = new FileOutputStream(fileTo);

            inputChannel = inputStream.getChannel();
            outputChannel = outputStream.getChannel();

            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
            closeQuietly(inputChannel);
            closeQuietly(outputChannel);
        }
    }

    /**
     * 检查源文件和目标文件
     *
     * @param fileFrom
     * @param fileTo
     * @return
     */
    private static boolean checkFile(File fileFrom, File fileTo) {
        if (fileFrom == null || !fileFrom.exists())
            return false;

        if (fileFrom.isDirectory())
            throw new IllegalArgumentException("fileFrom must not be a directory");

        if (fileTo == null)
            return false;

        if (fileTo.exists()) {
            if (fileTo.isDirectory()) {
                throw new IllegalArgumentException("fileTo must not be a directory");
            } else {
                if (!fileTo.delete())
                    return false;
            }
        }

        final File fileToParent = fileTo.getParentFile();
        if (fileToParent != null && !fileToParent.exists()) {
            if (!fileToParent.mkdirs())
                return false;
        }
        return true;
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
