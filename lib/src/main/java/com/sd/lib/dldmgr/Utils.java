package com.sd.lib.dldmgr;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Utils
{
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable)
    {
        if (Looper.myLooper() == Looper.getMainLooper())
            runnable.run();
        else
            HANDLER.post(runnable);
    }

    public static File getCacheDir(String dirName, Context context)
    {
        File dir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            dir = new File(context.getExternalCacheDir(), dirName);
        } else
        {
            dir = new File(context.getCacheDir(), dirName);
        }
        return dir;
    }

    public static boolean mkdirs(File dir)
    {
        if (dir == null)
            return false;

        if (dir.exists())
            return true;

        try
        {
            return dir.mkdirs();
        } catch (Exception e)
        {
            return false;
        }
    }

    public static String MD5(String value)
    {
        try
        {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(value.getBytes());
            final byte[] bytes = messageDigest.digest();

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++)
            {
                final String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1)
                    sb.append('0');

                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String getExt(String url)
    {
        String ext = null;
        try
        {
            ext = MimeTypeMap.getFileExtensionFromUrl(url);
            if (!TextUtils.isEmpty(ext))
                ext = "." + ext;
        } catch (Exception e)
        {
        }
        return ext;
    }
}
