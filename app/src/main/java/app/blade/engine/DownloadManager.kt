package app.blade.engine

import android.content.Context
import android.os.Environment
import android.webkit.URLUtil
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import app.blade.data.DownloadDao
import app.blade.data.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Singleton
class DownloadManager @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao
) {
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val downloads = downloadDao.getAllDownloads()

    fun downloadFile(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

        downloadScope.launch {
            val downloadId = downloadDao.insertDownload(
                DownloadEntity(
                    url = url,
                    fileName = fileName,
                    totalSize = 0,
                    downloadedSize = 0,
                    status = DownloadEntity.STATUS_PENDING,
                    mimeType = mimeType
                )
            )

            try {
                val request = Request.Builder()
                    .url(url)
                    .apply { userAgent?.let { header("User-Agent", it) } }
                    .build()

                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)!!
                        .copy(status = DownloadEntity.STATUS_DOWNLOADING)
                )

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val body = response.body
                    val totalSize = body.contentLength()
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()

                    val file = File(downloadsDir, fileName)
                    var downloadedSize = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            var lastUpdate = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedSize += bytesRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastUpdate > 500) {
                                    downloadDao.updateDownload(
                                        downloadDao.getDownloadById(downloadId)!!.copy(
                                            downloadedSize = downloadedSize,
                                            totalSize = totalSize
                                        )
                                    )
                                    lastUpdate = currentTime
                                }
                            }
                        }
                    }

                    downloadDao.updateDownload(
                        downloadDao.getDownloadById(downloadId)!!.copy(
                            status = DownloadEntity.STATUS_COMPLETED,
                            downloadedSize = totalSize,
                            totalSize = totalSize
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed", e)
                downloadDao.getDownloadById(downloadId)?.let {
                    downloadDao.updateDownload(it.copy(status = DownloadEntity.STATUS_FAILED))
                }
            }
        }
    }

    fun deleteDownload(download: DownloadEntity) {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, download.fileName)
        if (file.exists()) file.delete()
        downloadScope.launch {
            downloadDao.deleteDownload(download)
        }
    }

    fun clearAll() {
        downloadScope.launch {
            downloadDao.clearAllDownloads()
        }
    }
}
