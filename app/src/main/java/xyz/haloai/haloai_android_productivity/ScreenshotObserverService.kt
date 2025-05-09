package xyz.haloai.haloai_android_productivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.haloai.haloai_android_productivity.ui.viewmodel.AssistantModeFunctionsViewModel
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class ScreenshotObserverService: Service(), KoinComponent {

    private lateinit var screenshotObserver: ContentObserver
    private val assistantModeFunctionsViewModel: AssistantModeFunctionsViewModel by inject()

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startScreenshotObserver()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(screenshotObserver)
    }

    private fun startForegroundService() {
        val channelId = "ScreenshotObserverServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screenshot Observer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Halo's here to help!")
            .setContentText("Take a screenshot, I'll take care of the rest.")
            .setSmallIcon(R.drawable.haloai_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2, notification)
    }

    private fun startScreenshotObserver() {
        val handler = Handler(Looper.getMainLooper())
        screenshotObserver = object : ContentObserver(handler) {
            private var lastScreenshotPath: String? = null
            private var lastScreenshotTime: Long = 0
            private var lastScreenshotFileSize: Long = 0
            private var handlerToken = "screenshots"

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                uri?.let {
                    val cursor = contentResolver.query(
                        uri,
                        arrayOf(MediaStore.Images.Media.DATA),
                        null,
                        null,
                        null
                    )

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                            val fileSize = File(filePath).length() // in bytes
                            if (filePath.contains("Screenshots")) {
                                handleNewScreenshot(filePath, fileSize)
                            }
                        }
                    }
                }
            }

            private fun handleNewScreenshot(filePath: String, fileSize: Long) {
                val currentTime = System.currentTimeMillis()
                // Skip if pending
                if (filePath.contains(".pending")) {
                    Log.d("ScreenshotObserver", "Pending file detected, skipping: $filePath")
                    return
                }
                if (lastScreenshotPath != null && (currentTime - lastScreenshotTime) < 10000) {
                    if (filePath == lastScreenshotPath) {
                        // Scrolling screenshot detected
                        lastScreenshotFileSize = fileSize
                        lastScreenshotTime = currentTime
                        // Reschedule the processing
                        handler.removeCallbacksAndMessages(handlerToken)
                        scheduleProcessing()
                    } else {
                        // Process the previous screenshot
                        handler.removeCallbacksAndMessages(handlerToken) // Cancel the previous processing
                        CoroutineScope(Dispatchers.IO).launch {
                            processScreenshot(lastScreenshotPath!!)
                        }
                        // Update for the new screenshot
                        lastScreenshotPath = filePath
                        lastScreenshotFileSize = fileSize
                        lastScreenshotTime = currentTime
                        scheduleProcessing()
                    }
                } else {
                    // No recent screenshots, handle the new one
                    lastScreenshotPath = filePath
                    lastScreenshotFileSize = fileSize
                    lastScreenshotTime = currentTime
                    scheduleProcessing()
                }
            }

            private fun scheduleProcessing() {
                handler.postDelayed({
                    if (System.currentTimeMillis() - lastScreenshotTime >= 10000) {
                        lastScreenshotPath?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                processScreenshot(filePath = it)
                            }
                        }
                        lastScreenshotPath = null
                        lastScreenshotFileSize = 0
                        lastScreenshotTime = 0
                    }
                }, handlerToken, 10000)
            }



            private suspend fun processScreenshot(filePath: String) {
                // Add your screenshot processing logic here
                if (filePath.contains(".pending")) {
                    Log.d("ScreenshotObserver", "Pending file detected, skipping: $filePath")
                    return
                }

                val finalFile = File(filePath)
                Log.d("ScreenshotObserver", "New screenshot detected: $filePath")
                val maxRetries = 5
                var retries = 0
                var bitmap: Bitmap? = null

                while (retries < maxRetries) {
                    if (finalFile.exists() && finalFile.isFile) {
                        try {
                            bitmap = BitmapFactory.decodeFile(filePath)
                            if (bitmap != null) {
                                break
                            }
                        } catch (e: FileNotFoundException) {
                            Log.d(
                                "ScreenshotObserver",
                                "File not found, retrying... ($retries/$maxRetries)"
                            )
                        } catch (e: IOException) {
                            Log.d(
                                "ScreenshotObserver",
                                "IOException while decoding file: ${e.message}"
                            )
                        }
                    }
                    else {
                        Log.d("ScreenshotObserver", "File does not exist, retrying... ($retries/$maxRetries)")
                    }

                    retries++
                    delay(1000)
                }

                if (bitmap != null) {
                    // Add your further processing logic here
                    Log.d("ScreenshotObserver", "Bitmap created for screenshot: $filePath")
                    val coroutineScope = CoroutineScope(Dispatchers.IO)
                    assistantModeFunctionsViewModel.processScreenshot(bitmap)
                    // val text = textExtractionFromImageViewModel.getTextFromBitmap(bitmap!!, coroutineScope)
                    // Log.d("ScreenshotObserver", "Extracted text: $text")
                } else {
                    Log.d("ScreenshotObserver", "Failed to create bitmap for screenshot after $maxRetries retries: $filePath")
                }

            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )
    }

}