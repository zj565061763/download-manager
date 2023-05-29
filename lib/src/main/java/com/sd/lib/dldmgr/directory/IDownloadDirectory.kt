package com.sd.lib.dldmgr.directory

import java.io.File

/**
 * 下载目录
 */
interface IDownloadDirectory {
    /**
     * 返回[url]对应的文件，如果文件不存在则返回[defaultFile]
     */
    fun urlFile(url: String?, defaultFile: File? = null): File?

    /**
     * 返回[url]对应的缓存文件
     *
     * @return null-文件不存在；不为null-文件存在
     */
    fun urlTempFile(url: String?): File?

    /**
     * 拷贝[file]文件到当前目录
     *
     * @return 拷贝成功-返回拷贝后的文件；拷贝失败-返回原文件
     */
    fun copyFile(file: File): File

    /**
     * 移动[file]文件到当前目录
     *
     * @return 移动成功-返回移动后的文件；移动失败-返回原文件
     */
    fun takeFile(file: File): File

    /**
     * 删除文件（临时文件[TempExt]不会被删除）
     * @param ext 文件扩展名（例如mp3）null-删除所有文件；空字符串-删除扩展名为空的文件
     * @return 返回删除的文件数量
     */
    fun deleteFile(ext: String?): Int

    /**
     * 删除临时文件
     *
     * @return 返回删除的文件数量
     */
    fun deleteTempFile(interceptor: FileInterceptor?): Int

    interface FileInterceptor {
        /**
         * 拦截文件[file]
         *
         * @return true-拦截
         */
        fun intercept(file: File): Boolean
    }

    companion object {
        /** 临时文件扩展名  */
        const val TempExt = "temp"
    }
}