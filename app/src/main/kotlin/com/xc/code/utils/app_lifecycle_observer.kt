package com.xc.code.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.xc.code.xc_application
import com.xc.code.service.keep_alive_service

class app_lifecycle_observer(private val app: xc_application) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        app.keep_alive_service_?.hide_notification()
    }

    override fun onStop(owner: LifecycleOwner) {
        app.keep_alive_service_?.show_notification()
    }

    companion object {
        fun init(app: xc_application) {
            val observer = app_lifecycle_observer(app)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }
}