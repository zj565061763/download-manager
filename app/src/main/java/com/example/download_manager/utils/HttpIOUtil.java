package com.example.download_manager.utils;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class HttpIOUtil
{
    private HttpIOUtil()
    {
    }

    public static String readString(InputStream in, String charset) throws IOException
    {
        if (TextUtils.isEmpty(charset))
            charset = "UTF-8";

        if (!(in instanceof BufferedInputStream))
            in = new BufferedInputStream(in);

        final Reader reader = new InputStreamReader(in, charset);
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int len;
        while ((len = reader.read(buffer)) >= 0)
        {
            sb.append(buffer, 0, len);
        }
        return sb.toString();
    }

    public static void writeString(OutputStream out, String content, String charset) throws IOException
    {
        if (TextUtils.isEmpty(charset))
            charset = "UTF-8";

        final Writer writer = new OutputStreamWriter(out, charset);
        writer.write(content);
        writer.flush();
    }

    public static void copy(InputStream in, OutputStream out, ProgressCallback callback) throws IOException
    {
        if (!(in instanceof BufferedInputStream))
            in = new BufferedInputStream(in);

        if (!(out instanceof BufferedOutputStream))
            out = new BufferedOutputStream(out);

        long count = 0;
        int len = 0;
        final byte[] buffer = new byte[1024];
        while ((len = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, len);
            count += len;

            if (callback != null)
                callback.onProgress(count);
        }
        out.flush();
    }

    public static void closeQuietly(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            } catch (Throwable ignored)
            {
            }
        }
    }

    public interface ProgressCallback
    {
        void onProgress(long count);
    }
}