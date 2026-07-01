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
        createFloatingWidget()
        handler.post(statsUpdater)
    }

    private fun createFloatingWidget() {
        // Base container FrameLayout
        val frameLayout = FrameLayout(this)
        
        // Rounded semi-transparent dark container
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            
            // Neon border with Slate glass background
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#E60B1216")) // 90% opacity deep dark background
                cornerRadius = dpToPx(18).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#3300E6FF")) // Subtle neon-cyan glowing outline
            }
            background = backgroundDrawable
        }

        // 1. Tiny handle indicator / logo
        val handleView = TextView(this).apply {
            text = "⚡"
            textSize = 12f
            setPadding(0, 0, dpToPx(6), 0)
        }
        contentLayout.addView(handleView)

        // 2. CPU stat
        val tvCpu = TextView(this).apply {
            tag = "overlay_cpu"
            text = "CPU: 0%"
            textColorHex("#00FF66") // Neon green
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dpToPx(10), 0)
        }
        contentLayout.addView(tvCpu)

        // Divider
        val tvDivider = TextView(this).apply {
            text = "|"
            textColorHex("#232D34")
            textSize = 10f
            setPadding(0, 0, dpToPx(10), 0)
        }
        contentLayout.addView(tvDivider)

        // 3. RAM stat
        val tvRam = TextView(this).apply {
            tag = "overlay_ram"
            text = "RAM: 0%"
            textColorHex("#00E6FF") // Neon cyan
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dpToPx(10), 0)
        }
        contentLayout.addView(tvRam)

        // 4. Compact Dismiss Button (X)
        val btnClose = ImageView(this).apply {
            val xDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#33FF0055")) // 20% opacity hot pink
                cornerRadius = dpToPx(10).toFloat()
            }
            background = xDrawable
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            
            contentDescription = "Close Monitor"
            minimumWidth = dpToPx(18)
            minimumHeight = dpToPx(18)
            
            setOnClickListener {
                stopSelf()
            }
        }
        
        // Small helper text close overlay button
        val tvCloseSymbol = TextView(this).apply {
            text = "×"
            textColorHex("#FF3366")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        
        val closeContainer = FrameLayout(this).apply {
            addView(btnClose)
            addView(tvCloseSymbol, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            ))
            setOnClickListener {
                stopSelf()
            }
        }
        contentLayout.addView(closeContainer, LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)))

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

        windowManager.addView(floatingView, params)
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
