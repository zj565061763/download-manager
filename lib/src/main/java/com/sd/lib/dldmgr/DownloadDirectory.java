package com.sd.lib.dldmgr;

import android.text.TextUtils;

import java.io.File;

public class DownloadDirectory implements IDownloadDirectory
{
    private static final String EXT_TEMP = ".temp";
    private final File mDirectory;

    public DownloadDirectory(File directory)
    {
        if (directory == null)
            throw new NullPointerException("directory is null");
        mDirectory = directory;
    }

    @Override
    public boolean copyFile(File file)
    {
        if (!Utils.checkDir(mDirectory))
            return false;

        if (file == null || !file.exists())
            return false;

        if (file.isDirectory())
            throw new IllegalArgumentException("file must not be a directory");

        final String filename = file.getName();
        final File copyFile = new File(mDirectory, filename);
        Utils.delete(copyFile);

        final File tempFile = new File(mDirectory, filename + EXT_TEMP);
        if (Utils.copyFile(file, tempFile))
        {
            if (tempFile.renameTo(copyFile))
                return true;
        }
        return false;
    }

    @Override
    public int deleteFile(String ext)
    {
        if (!TextUtils.isEmpty(ext))
        {
            if (ext.startsWith("."))
                throw new IllegalArgumentException("Illegal ext start with dot:" + ext);
        }

        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return 0;

        int count = 0;
        boolean delete = false;
        for (File file : files)
        {
            final String name = file.getName();
            if (name.endsWith(EXT_TEMP))
                continue;

            if (ext == null)
            {
                delete = true;
            } else
            {
                final String itemExt = Utils.getExt(file.getAbsolutePath());
                if (ext.equals(itemExt))
                    delete = true;
            }

            if (delete)
            {
                if (Utils.delete(file))
                    count++;
            }
        }
        return count;
    }

    @Override
    public int deleteTempFile(FileInterceptor interceptor)
    {
        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return 0;

        int count = 0;
        for (File file : files)
        {
            if (interceptor != null && interceptor.intercept(file))
                continue;

            final String name = file.getName();
            if (name.endsWith(EXT_TEMP))
            {
                if (Utils.delete(file))
                    count++;
            }
        }
        return count;
    }

    private File[] getAllFile()
    {
        if (!Utils.checkDir(mDirectory))
            return null;

        final File[] files = mDirectory.listFiles();
        if (files == null || files.length <= 0)
            return null;

        return files;
    }
}
