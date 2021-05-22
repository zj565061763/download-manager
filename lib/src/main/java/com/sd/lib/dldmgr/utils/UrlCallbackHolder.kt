package com.sd.lib.dldmgr.utils

import android.util.Log
import com.sd.lib.dldmgr.DownloadManagerConfig
import com.sd.lib.dldmgr.IDownloadManager

internal class UrlCallbackHolder {
    private val _mapCallback: MutableMap<String, MutableSet<IDownloadManager.Callback>> = HashMap()
    private val _mapCallbackUrl: MutableMap<IDownloadManager.Callback, String> = HashMap()

    @Synchronized
    fun add(url: String, callback: IDownloadManager.Callback) {
        val cacheUrl = _mapCallbackUrl[callback]
        if (url == cacheUrl) {
            // 对象已经添加过了
            return
        }

        // 如果对象之前监听的是别的url，需要先移除，因为一个对象只允许监听一个url
        remove(callback)

        var holder = _mapCallback[url]
        if (holder == null) {
            holder = HashSet()
            _mapCallback[url] = holder
        }

        if (holder.add(callback)) {
            _mapCallbackUrl[callback] = url
        }

        if (DownloadManagerConfig.get().isDebug) {
            Log.i(
                UrlCallbackHolder::class.java.simpleName, "add url:${url} callback:${callback}"
                        + " sizeUrl:${_mapCallback.size} sizeCallback:${_mapCallbackUrl.size}"
            )
        }
    }

    @Synchronized
    fun remove(callback: IDownloadManager.Callback): Boolean {
        val url = _mapCallbackUrl.remove(callback) ?: return false
        var holder = _mapCallback[url] ?: return false

        val remove = holder.remove(callback)
        if (holder.isEmpty()) {
            _mapCallback.remove(url)
        }

        if (remove) {
            if (DownloadManagerConfig.get().isDebug) {
                Log.i(
                    UrlCallbackHolder::class.java.simpleName, "remove url:${url} callback:${callback}"
                            + " sizeUrl:${_mapCallback.size} sizeCallback:${_mapCallbackUrl.size}"
                )
            }
        }
        return remove
    }

    @Synchronized
    fun removeUrl(url: String): Array<IDownloadManager.Callback>? {
        var holder = _mapCallback.remove(url) ?: return null
        return holder.toTypedArray()
    }
}