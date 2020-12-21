package com.sd.lib.dldmgr;

import java.io.File;

/**
 * 下载目录
 */
public interface IDownloadDirectory
{
    /** 临时文件扩展名 */
    String EXT_TEMP = ".temp";

    /**
     * 返回url对应的文件
     *
     * @param url
     * @return null-文件不存在，不为null文件存在
     */
    File getFile(String url);

    /**
     * 返回url对应的缓存文件
     *
     * @param url
     * @return null-文件不存在，不为null文件存在
     */
    File getTempFile(String url);

    /**
     * 新建url对应的文件
     *
     * @param url
     * @return
     */
    File newUrlFile(String url);

    /**
     * 新建url对应的临时文件
     *
     * @param url
     * @return
     */
    File newUrlTempFile(String url);

    /**
     * 复制文件到当前目录
     *
     * @param file
     * @return
     */
    boolean copyFile(File file);

    /**
     * 删除文件（临时文件不会被删除）
     * <p>
     * 如果指定了扩展名，则扩展名不能包含点符号：<br>
     * 合法：mp3<br>
     * 不合法：.mp3
     *
     * @param ext 文件扩展名(例如mp3)；null-删除所有文件；空字符串-删除扩展名为空的文件
     * @return 返回删除的文件数量
     */
    int deleteFile(String ext);

    /**
     * 删除临时文件
     *
     * @param interceptor
     * @return
     */
    int deleteTempFile(FileInterceptor interceptor);

    interface FileInterceptor
    {
        /**
         * 拦截文件
         *
         * @param file
         * @return true-拦截
         */
        boolean intercept(File file);
    }
}
