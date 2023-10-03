package com.example.servicesample

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAG_BOUND = "MyBoundService"

class MyBoundService : Service() {
    private var launch: Job? = null
    private var binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG_BOUND, "onCreate")
    }

    inner class LocalBinder : Binder() {
        fun getService(): MyBoundService = this@MyBoundService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG_BOUND, "onStartCommand intent: $intent flags: $flags startId $startId")
        return START_STICKY
    }

    fun getData(data: (data: Int) -> Unit) {
        launch = CoroutineScope(Dispatchers.Main).launch {
            (0..8000).forEach {
                delay(1000)
                Log.d(TAG_BOUND, "service data $it")
                data(it)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        Log.d(TAG_BOUND, "onBind")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG_BOUND, "onDestroy")
        launch?.cancel()
        super.onDestroy()
    }
}