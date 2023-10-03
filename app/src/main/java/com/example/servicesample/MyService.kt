package com.example.servicesample

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAG = "MyService"

class MyService : Service() {
    private var launch: Job? = null
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand intent: $intent flags: $flags startId $startId")
        if (launch == null) {
            launch = CoroutineScope(Dispatchers.Main).launch {
                (0..8000).forEach {
                    delay(1000)
                    Log.d(TAG, "service data $it")
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        launch?.cancel()
        super.onDestroy()
    }
}