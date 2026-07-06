package com.jmwl.gostudio.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.jmwl.gostudio.gostudio_application
import com.jmwl.gostudio.service.keep_alive_service

class app_lifecycle_observer(private val app: gostudio_application) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        app.keep_alive_service_?.hide_notification()
    }

    override fun onStop(owner: LifecycleOwner) {
        app.keep_alive_service_?.show_notification()
    }

    companion object {
        fun init(app: gostudio_application) {
            val observer = app_lifecycle_observer(app)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }
}