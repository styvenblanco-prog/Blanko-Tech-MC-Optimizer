package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val optimizerEngine = OptimizationEngine()

    private val statsUpdater = object : Runnable {
        override fun run() {
            floatingView?.let { view ->
                val tvCpu = view.findViewWithTag<TextView>("overlay_cpu")
                val tvRam = view.findViewWithTag<TextView>("overlay_ram")

                val cpuVal = optimizerEngine.getCpuUsage()
                val (ramVal, _) = optimizerEngine.getRamStats(applicationContext)

                tvCpu?.text = "CPU: $cpuVal%"
                tvRam?.text = "RAM: $ramVal%"
            }
            handler.postDelayed(this, 1500) // update every 1.5s
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            createFloatingWidget()
            handler.post(statsUpdater)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun createFloatingWidget() {
        // Base container FrameLayout
        val frameLayout = FrameLayout(this)
        
        // Simple semi-transparent dark container
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#B3000000")) // Simple 70% dark background
                cornerRadius = dpToPx(6).toFloat()
            }
            background = backgroundDrawable
        }

        // 1. CPU stat
        val tvCpu = TextView(this).apply {
            tag = "overlay_cpu"
            text = "CPU: 0%"
            textColorHex("#FFFFFF")
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dpToPx(6), 0)
        }
        contentLayout.addView(tvCpu)

        // Divider
        val tvDivider = TextView(this).apply {
            text = "|"
            textColorHex("#66FFFFFF")
            textSize = 10f
            setPadding(0, 0, dpToPx(6), 0)
        }
        contentLayout.addView(tvDivider)

        // 2. RAM stat
        val tvRam = TextView(this).apply {
            tag = "overlay_ram"
            text = "RAM: 0%"
            textColorHex("#FFFFFF")
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dpToPx(8), 0)
        }
        contentLayout.addView(tvRam)

        // 3. Simple Close Button (X)
        val tvClose = TextView(this).apply {
            text = "×"
            textColorHex("#FF3366")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            setOnClickListener {
                stopSelf()
            }
        }
        contentLayout.addView(tvClose)

        frameLayout.addView(contentLayout)
        floatingView = frameLayout

        // Layout parameters
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Custom drag listener
        contentLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun TextView.textColorHex(hexColor: String) {
        setTextColor(Color.parseColor(hexColor))
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(statsUpdater)
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View already removed
            }
        }
    }
}
