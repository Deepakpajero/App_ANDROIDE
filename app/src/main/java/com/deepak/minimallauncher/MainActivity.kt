package com.deepak.minimallauncher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var home: LinearLayout
    private lateinit var appList: LinearLayout
    private lateinit var drawer: LinearLayout
    private lateinit var pm: android.content.pm.PackageManager

    private val prefs by lazy {
        getSharedPreferences("minimal_launcher", Context.MODE_PRIVATE)
    }

    // S23 Ultra / Samsung-friendly defaults.
    // The launcher also automatically falls back to matching app labels.
    private val defaults = linkedMapOf(
        "com.samsung.android.dialer" to "Phone",
        "com.samsung.android.messaging" to "Messages",
        "com.google.android.apps.maps" to "Maps",
        "com.sec.android.app.camera" to "Camera",
        "com.google.android.apps.nbu.paisa.user" to "GPay",
        "com.google.android.apps.authenticator2" to "Authenticator"
    )

    private var selectedPackages = mutableListOf<String>()
    private var delayedPackages = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        pm = packageManager

        loadPreferences()
        buildShell()
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (::appList.isInitialized) populateHome()
    }

    private fun loadPreferences() {
        val saved = prefs.getStringSet("home_apps", null)
        selectedPackages = (saved?.toMutableList() ?: defaults.keys.toMutableList())
        delayedPackages = prefs.getStringSet("delayed_apps", emptySet())!!.toMutableSet()
    }

    private fun savePreferences() {
        prefs.edit()
            .putStringSet("home_apps", selectedPackages.toSet())
            .putStringSet("delayed_apps", delayedPackages)
            .apply()
    }

    private fun buildShell() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(35), dp(28), dp(18))
        }
        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(35), dp(28), dp(18))
            setBackgroundColor(Color.BLACK)
        }
        root.addView(home, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        root.setOnTouchListener(SwipeListener())
    }

    private fun showHome() {
        drawer.visibility = View.GONE
        home.visibility = View.VISIBLE
        populateHome()
    }

    private fun populateHome() {
        home.removeAllViews()

        val clock = tv(23f, Color.WHITE).apply {
            gravity = Gravity.CENTER
        }
        val date = tv(13f, 0xFF999999.toInt()).apply {
            gravity = Gravity.CENTER
        }

        home.addView(clock, lp(-1, 42))
        home.addView(date, lp(-1, 28))

        fun updateTime() {
            val now = Date()
            clock.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
            date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
        }
        updateTime()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (home.visibility == View.VISIBLE) updateTime()
                handler.postDelayed(this, 30_000)
            }
        }, 30_000)

        val line = View(this).apply { setBackgroundColor(0xFF303030.toInt()) }
        val lineLp = lp(-1, 1)
        lineLp.setMargins(0, dp(24), 0, dp(20))
        home.addView(line, lineLp)

        home.addView(appList, lp(-1, 0, 1f))

        val all = tv(15f, 0xFFBBBBBB.toInt()).apply {
            text = "All Apps  →"
            gravity = Gravity.CENTER
            setOnClickListener { showDrawer() }
            setOnLongClickListener {
                showLauncherSettings()
                true
            }
        }
        home.addView(all, lp(-1, 62))
    }

    private fun populateHomeApps() {
        appList.removeAllViews()
        val available = launcherApps().associateBy { it.activityInfo.packageName }

        selectedPackages.forEach { pkg ->
            val info = available[pkg] ?: return@forEach
            val label = info.loadLabel(pm).toString()
            val b = tv(18f, Color.WHITE).apply {
                text = label
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
                setOnClickListener { openWithOptionalDelay(pkg, label) }
            }
            appList.addView(b, lp(-1, 57))
        }

        // Re-populate after the shell is ready.
        if (appList.childCount == 0) {
            val hint = tv(14f, 0xFF777777.toInt()).apply {
                text = "Long-press “All Apps” to choose your apps"
                gravity = Gravity.CENTER
            }
            appList.addView(hint, lp(-1, 80))
        }
    }

    private fun showDrawer() {
        home.visibility = View.GONE
        drawer.visibility = View.VISIBLE
        drawer.removeAllViews()

        val header = tv(21f, Color.WHITE).apply {
            text = "All Apps"
            setPadding(0, 0, 0, dp(18))
        }
        drawer.addView(header, lp(-1, 45))

        val search = EditText(this).apply {
            hint = "Search"
            hintTextColor = 0xFF666666.toInt()
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(4), 0, dp(4), 0)
        }
        drawer.addView(search, lp(-1, 48))

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply {
            addView(listContainer)
        }
        drawer.addView(scroll, lp(-1, 0, 1f))

        fun render(filter: String) {
            listContainer.removeAllViews()
            launcherApps()
                .filter {
                    it.loadLabel(pm).toString().contains(filter, ignoreCase = true)
                }
                .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }
                .forEach { info ->
                    val pkg = info.activityInfo.packageName
                    val row = tv(17f, Color.WHITE).apply {
                        text = info.loadLabel(pm).toString()
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(8), 0, dp(8), 0)
                        setOnClickListener { openWithOptionalDelay(pkg, text.toString()) }
                    }
                    listContainer.addView(row, lp(-1, 52))
                }
        }

        render("")
        search.addTextChangedListener(SimpleTextWatcher { render(it) })

        val settings = tv(14f, 0xFF777777.toInt()).apply {
            text = "Launcher settings"
            gravity = Gravity.CENTER
            setOnClickListener { showLauncherSettings() }
        }
        drawer.addView(settings, lp(-1, 55))
    }

    private fun showLauncherSettings() {
        val choices = launcherApps().sortedBy { it.loadLabel(pm).toString().lowercase() }
        val labels = choices.map { it.loadLabel(pm).toString() }.toTypedArray()
        val checked = choices.map { selectedPackages.contains(it.activityInfo.packageName) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Home screen apps")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val pkg = choices[which].activityInfo.packageName
                if (isChecked && !selectedPackages.contains(pkg)) selectedPackages.add(pkg)
                if (!isChecked) selectedPackages.remove(pkg)
                // Limit prevents the minimalist home screen becoming a normal launcher.
                if (selectedPackages.size > 7) {
                    selectedPackages.remove(pkg)
                    Toast.makeText(this, "Maximum 7 home apps", Toast.LENGTH_SHORT).show()
                }
                savePreferences()
            }
            .setPositiveButton("Save") { _, _ -> showHome() }
            .setNeutralButton("Default Home") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                } catch (_: Exception) {}
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openWithOptionalDelay(pkg: String, label: String) {
        if (!delayedPackages.contains(pkg)) {
            launch(pkg)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Open $label?")
            .setMessage("This app has an intentional delay to reduce automatic checking.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open") { _, _ -> launch(pkg) }
            .show()
    }

    private fun launch(pkg: String) {
        try {
            pm.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
        } catch (_: Exception) {}
    }

    private fun launcherApps(): List<ResolveInfo> =
        pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            android.content.pm.PackageManager.MATCH_ALL
        )

    private fun tv(size: Float, color: Int) = TextView(this).apply {
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }

    private fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, if (h == 0) 0 else dp(h), weight)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private inner class SwipeListener : View.OnTouchListener {
        private var downY = 0f
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = e.rawY
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (downY - e.rawY > dp(70)) showDrawer()
                    else if (e.rawY - downY > dp(70) && drawer.visibility == View.VISIBLE) showHome()
                    return true
                }
            }
            return true
        }
    }

    private class SimpleTextWatcher(val changed: (String) -> Unit) :
        android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
            changed(s?.toString() ?: "")
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
