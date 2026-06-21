package com.freezescreen.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.freezescreen.app.databinding.FloatingBubbleBinding

class FreezeService : Service() {

    companion object {
        var mediaProjectionResultCode: Int = 0
        var mediaProjectionData: Intent? = null
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var selectionOverlay: SelectionOverlayView? = null
    private var freezeOverlay: View? = null
    private var mediaProjection: MediaProjection? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())
        showFloatingIcon()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "freeze_service",
                "Freeze Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "freeze_service")
            .setContentTitle("Freeze Screen")
            .setContentText("Tap the bubble to freeze")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingIcon() {
        if (floatingView != null) return

        val bubbleBinding = FloatingBubbleBinding.inflate(LayoutInflater.from(this))
        floatingView = bubbleBinding.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        floatingView?.setOnClickListener {
            if (selectionOverlay != null) {
                // Already selecting, ignore
                return@setOnClickListener
            }
            if (freezeOverlay != null) {
                // Remove frozen overlay
                removeFreezeOverlay()
                return@setOnClickListener
            }
            // Start selection mode
            startSelection()
        }

        windowManager.addView(floatingView, params)
    }

    private fun startSelection() {
        if (mediaProjectionData == null) {
            handler.post {
                Toast.makeText(this, "Screen capture permission not granted yet. Open app again.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        selectionOverlay = SelectionOverlayView(this).apply {
            onSelectionCompleted = { rect ->
                // Capture screen region
                captureAndFreeze(rect)
                removeSelectionOverlay()
            }
            onCancel = {
                removeSelectionOverlay()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(selectionOverlay, params)
    }

    private fun removeSelectionOverlay() {
        selectionOverlay?.let {
            windowManager.removeView(it)
            selectionOverlay = null
        }
    }

    private fun captureAndFreeze(rect: Rect) {
        // Setup media projection for a single capture
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(
            mediaProjectionResultCode,
            mediaProjectionData!!.clone() as Intent
        )

        val width = rect.width()
        val height = rect.height()

        // Use ImageReader to grab a screenshot of the selected region
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)

        mediaProjection?.createVirtualDisplay(
            "FreezeCapture",
            width, height, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            object : VirtualDisplay.Callback() {
                override fun onPaused() {}
                override fun onResumed() {}
                override fun onStopped() {}
            }, null
        )

        // Wait a moment for the display to produce a frame
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                // Create bitmap
                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                // Crop to actual width
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                image.close()
                reader.close()
                mediaProjection?.stop()
                mediaProjection = null
                handler.post {
                    showFrozenOverlay(cropped, rect)
                }
            }
        }, handler)
    }

    private fun showFrozenOverlay(bitmap: Bitmap, rect: Rect) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.FIT_XY

        val params = WindowManager.LayoutParams(
            rect.width(),
            rect.height(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
        }

        windowManager.addView(imageView, params)
        freezeOverlay = imageView
    }

    private fun removeFreezeOverlay() {
        freezeOverlay?.let {
            windowManager.removeView(it)
            freezeOverlay = null
        }
    }

    override fun onDestroy() {
        removeSelectionOverlay()
        removeFreezeOverlay()
        floatingView?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
        super.onDestroy()
    }
}
