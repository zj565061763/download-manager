package com.sd.demo.download_manager

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.download_manager.databinding.ActivityMainBinding
import com.sd.lib.dldmgr.DownloadInfo
import com.sd.lib.dldmgr.DownloadRequest
import com.sd.lib.dldmgr.FDownloadManager
import com.sd.lib.dldmgr.IDownloadManager
import com.sd.lib.dldmgr.directory.DownloadDirectory
import com.sd.lib.dldmgr.directory.IDownloadDirectory
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var _binding: ActivityMainBinding

    private val _url = URL_SMALL
    private lateinit var _downloadDirectory: IDownloadDirectory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // 创建一个目录，下载成功后可以拷贝到该目录
        _downloadDirectory = DownloadDirectory.from(getExternalFilesDir("my_download"))

        // 添加下载回调
        FDownloadManager.default.addCallback(_downloadCallback)
    }

    /**
     * 下载回调
     */
    private val _downloadCallback: IDownloadManager.Callback = object : IDownloadManager.Callback {
        override fun onPrepare(info: DownloadInfo) {
            Log.i(TAG, "onPrepare:${info.url} state:${info.state}")
        }

        override fun onProgress(info: DownloadInfo) {
            // 下载参数
            val param = info.transmitParam
            // 下载进度
            val progress = param.progress
            // 下载速率
            val speed = param.speedKBps
            Log.i(TAG, "onProgress:${progress} ${speed} state:${info.state}")
        }

        override fun onSuccess(info: DownloadInfo, file: File) {
            Log.i(TAG, "onSuccess:${info.url} file:${file.absolutePath} state:${info.state}")

            val start = System.currentTimeMillis()
            _downloadDirectory.copyFile(file)
            Log.i(TAG, "process file time:${(System.currentTimeMillis() - start)}")
        }

        override fun onError(info: DownloadInfo) {
            Log.e(TAG, "onError:${info.error} throwable:${info.throwable} state:${info.state}")
        }
    }

    override fun onClick(v: View) {
        when (v) {
            _binding.btnDownload -> {
                // 创建下载请求对象
                val downloadRequest = DownloadRequest.Builder() // 设置断点下载，true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
                    .setPreferBreakpoint(true) // 下载地址
                    .build(_url)

                // 添加下载任务
                val addTask = FDownloadManager.default.addTask(downloadRequest)
                Log.i(TAG, "click download addTask:${addTask}")
            }
            _binding.btnCancel -> {
                // 取消下载任务
                FDownloadManager.default.cancelTask(_url)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除下载回调
        FDownloadManager.default.removeCallback(_downloadCallback)
        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloadManager.default.deleteTempFile()
        // 删除下载文件（临时文件不会被删除）
        FDownloadManager.default.deleteDownloadFile(null)

        _downloadDirectory.deleteTempFile(null)
        _downloadDirectory.deleteFile(null)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val URL_BIG = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"
        private const val URL_SMALL = "http://1251020758.vod2.myqcloud.com/8a96e57evodgzp1251020758/602d1d1a5285890800849942893/tRGP04QVdCEA.mp4"
    }
}