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
     * 删除文件（临时文件不会被删除）
     * @param ext 文件扩展名（例如mp3）null-删除所有文件；空字符串-删除扩展名为空的文件
     * @return 返回删除的文件数量
     */
    fun deleteFile(ext: String?): Int

    /**
     * 操作文件夹
     */
    fun <T> modify(block: (dir: File?) -> T): T
}