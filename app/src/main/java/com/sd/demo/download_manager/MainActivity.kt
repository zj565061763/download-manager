package com.sd.demo.download_manager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.download_manager.ui.theme.AppTheme
import com.sd.lib.dldmgr.DownloadInfo
import com.sd.lib.dldmgr.DownloadRequest
import com.sd.lib.dldmgr.FDownloadManager
import com.sd.lib.dldmgr.IDownloadManager
import com.sd.lib.dldmgr.directory.DownloadDirectory
import com.sd.lib.dldmgr.directory.IDownloadDirectory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private const val URL = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

class MainActivity : ComponentActivity() {
    private val _mainScope = MainScope()
    private lateinit var _downloadDirectory: IDownloadDirectory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    onClickDownload = {
                        download()
                    },
                    onClickCancel = {
                        cancelDownload()
                    },
                )
            }
        }

        // 创建一个目录，下载成功后可以拷贝到该目录
        _downloadDirectory = DownloadDirectory.from(getExternalFilesDir("my_download"))

        // 添加下载回调
        FDownloadManager.addCallback(_downloadCallback)
    }

    private fun download() {
        // 创建下载请求对象
        val downloadRequest = DownloadRequest.Builder() // 设置断点下载，true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
            .setPreferBreakpoint(true) // 下载地址
            .build(URL)

        // 添加下载任务
        val addTask = FDownloadManager.addTask(downloadRequest)
        logMsg { "click download addTask:${addTask}" }

        if (addTask) {
            _mainScope.launch {
                val file = FDownloadManager.awaitTask(URL)
                logMsg { "awaitTask file:${file}" }
            }
        }
    }

    private fun cancelDownload() {
        // 取消下载任务
        FDownloadManager.cancelTask(URL)
    }

    /**
     * 下载回调
     */
    private val _downloadCallback: IDownloadManager.Callback = object : IDownloadManager.Callback {
        override fun onPrepare(info: DownloadInfo) {
            logMsg { "onPrepare:${info.url} state:${info.state}" }
        }

        override fun onProgress(info: DownloadInfo) {
            // 下载参数
            val param = info.transmitParam
            // 下载进度
            val progress = param.progress
            // 下载速率
            val speed = param.speedKBps

            logMsg { "onProgress:${progress} $speed state:${info.state}" }
        }

        override fun onSuccess(info: DownloadInfo, file: File) {
            logMsg { "onSuccess:${info.url} file:${file.absolutePath} state:${info.state}" }

            val start = System.currentTimeMillis()
            _downloadDirectory.copyFile(file)

            logMsg { "process file time:${(System.currentTimeMillis() - start)}" }
        }

        override fun onError(info: DownloadInfo) {
            logMsg { "onError:${info.error} throwable:${info.throwable} state:${info.state}" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _mainScope.cancel()

        // 移除下载回调
        FDownloadManager.removeCallback(_downloadCallback)
        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloadManager.deleteTempFile()
        // 删除下载文件（临时文件不会被删除）
        FDownloadManager.deleteDownloadFile(null)

        _downloadDirectory.deleteTempFile(null)
        _downloadDirectory.deleteFile(null)
    }
}

@Composable
private fun Content(
    onClickDownload: () -> Unit,
    onClickCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = onClickDownload
        ) {
            Text(text = "download")
        }

        Button(
            onClick = onClickCancel
        ) {
            Text(text = "cancel")
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("download-manager-demo", block())
}