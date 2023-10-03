package com.example.servicesample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAG_FOREGROUND = "MyService"

class MyForegroundService : Service() {
    private var launch: Job? = null
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG_FOREGROUND, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG_FOREGROUND, "onStartCommand intent: $intent flags: $flags startId $startId")
        launch = CoroutineScope(Dispatchers.Main).launch {
            (0..8000).forEach {
                delay(1000)
                Log.d(TAG, "foreground service data $it")
            }
        }
        generateForegroundNotification()
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG_FOREGROUND, "onBind")
        return null
    }

    //Notification for ON-going
    private val mNotificationId = 123

    private fun generateForegroundNotification() {
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }


        val channelID = "MY_FOREGROUND_SERVICE_CHANNEL_$mNotificationId"
        val channel = NotificationChannel(
            channelID,
            "MY_FOREGROUND_SERVICE Core Service",
            NotificationManager.IMPORTANCE_LOW
        )

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val serviceName = "MyForegroundService"
        val notification: Notification = Notification.Builder(this, channelID)
            .setContentTitle(serviceName)
            .setContentText("$serviceName is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_launcher_foreground
                )
            )
            .build()
        startForeground(mNotificationId, notification)
    }


    override fun onDestroy() {
        Log.d(TAG_FOREGROUND, "onDestroy")
        launch?.cancel()
        super.onDestroy()
    }
}