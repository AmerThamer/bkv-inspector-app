package com.amerthamer.justicebringer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is FormActivity) {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                    activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                    activity.window.decorView.post {
                        val scroll = activity.findViewById<NestedScrollView?>(R.id.rootScroll)
                        val notes = activity.findViewById<View?>(R.id.editNotes)
                        if (scroll != null && notes != null) {
                            notes.setOnFocusChangeListener { v, hasFocus ->
                                if (hasFocus) {
                                    scroll.post {
                                        scroll.smoothScrollTo(0, v.bottom)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
