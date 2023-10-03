package com.example.servicesample

import RotationAIDLInterface
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avinash.aidl.AidlSensorService
import com.example.servicesample.ui.theme.ServiceSampleTheme


class MainActivity : ComponentActivity() {
    private lateinit var mService: MyBoundService
    private var mBound: Boolean = false
    private lateinit var aidlRotationalServiceIntent: Intent
    private var isAidlServiceConnected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "onServiceConnected ComponentName:$componentName IBinder:$binder")
            val mBinder = binder as MyBoundService.LocalBinder
            mService = mBinder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected ComponentName:$componentName")
            mBound = false
        }
    }

    private val aidlRotationalServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "AIDL onServiceConnected ComponentName:$componentName IBinder:$binder")
            val orientationInterface = RotationAIDLInterface.Stub.asInterface(binder)
            orientationInterface?.rotationalData
            isAidlServiceConnected = true
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.d(TAG, "AIDL onServiceDisconnected ComponentName:$componentName")
            isAidlServiceConnected = false
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate mBound: $mBound")
        setContent {
            ServiceSampleTheme {
                var dataState by rememberSaveable { mutableStateOf(-1) }
                var aidlDataState by rememberSaveable { mutableStateOf("") }
                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(text = "Services", color = Color.White) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = colorResource(
                                id = R.color.purple_500
                            )
                        )
                    )
                }) {
                    Box(modifier = Modifier.padding(it)) {
                        Services(
                            dataState, aidlDataState
                        ) { startStop: Boolean, eventType: EventType ->
                            Log.d(TAG, "startStop $startStop eventType $eventType")
                            when (eventType) {
                                EventType.SERVICE -> {
                                    handleServiceData(startStop)
                                }

                                EventType.BOUND -> {
                                    if (!startStop) dataState = -1
                                    handleBoundServiceData(startStop)
                                }

                                EventType.FOREGROUND -> {
                                    handleForegroundServiceData(startStop)
                                }

                                EventType.AIDL -> {
                                    handleAidlServiceData(startStop)
                                    if (startStop) {
                                        AidlSensorService.mSensorDataCallback = { sensorData ->
                                            Log.d(TAG, "mSensorDataCallback sensorData:$sensorData")
                                            aidlDataState = sensorData.contentToString()
                                        }
                                    } else {
                                        aidlDataState = ""
                                    }
                                }

                                EventType.BOUND_DATA -> {
                                    if (mBound) {
                                        mService.getData {
                                            dataState = it
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleServiceData(startStop: Boolean) {
        Log.d(TAG, "handleServiceData startStop:$startStop")
        Intent(this, MyService::class.java).apply {
            if (startStop) {
                startService(this)
            } else {
                stopService(this)
            }
        }
    }

    private fun handleBoundServiceData(startStop: Boolean) {
        Log.d(TAG, "handleBoundServiceData startStop:$startStop")
        if (startStop) {
            // Bind to LocalService.
            Intent(this, MyBoundService::class.java).also { intent ->
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        } else {
            unBind()
        }
    }

    private fun handleForegroundServiceData(startStop: Boolean) {
        Log.d(TAG, "handleForegroundServiceData startStop:$startStop")
        Intent(this, MyForegroundService::class.java).also { intent ->
            if (startStop) {
                if (!foregroundServiceRunning()) {
                    startService(intent)
                }
            } else {
                stopService(intent)
            }
        }
    }

    private fun handleAidlServiceData(startStop: Boolean) {
        Log.d(TAG, "handleAidlServiceData startStop:$startStop")
        Intent(this, AidlSensorService::class.java).also { intent ->
            aidlRotationalServiceIntent = intent
            if (startStop) {
                bindService(
                    aidlRotationalServiceIntent,
                    aidlRotationalServiceConnection,
                    Context.BIND_AUTO_CREATE
                )
            } else {
                unBindAidlService()
            }
        }
    }

    private fun unBind() {
        Log.d(TAG, "unBind")
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
    }

    private fun unBindAidlService() {
        Log.d(TAG, "unBindAidlService")
        if (isAidlServiceConnected) {
            unbindService(aidlRotationalServiceConnection)
            stopService(aidlRotationalServiceIntent)
            isAidlServiceConnected = false
        }
    }

    enum class EventType {
        SERVICE, BOUND, BOUND_DATA, FOREGROUND, AIDL
    }

    private fun foregroundServiceRunning(): Boolean {
        Log.d(TAG, "foregroundServiceRunning")
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (MyForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        unBind()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
fun Services(
    dataState: Int,
    aidlDataState: String,
    onClick: (startStop: Boolean, EventType: MainActivity.EventType) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f, false)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
            Text(text = "Normal Service")
            Row(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { onClick(true, MainActivity.EventType.SERVICE) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Start")
                }

                Button(
                    onClick = { onClick(false, MainActivity.EventType.SERVICE) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Stop")
                }
            }

            Text(text = "Bound Service")
            Row(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { onClick(true, MainActivity.EventType.BOUND) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Start")
                }

                Button(
                    onClick = { onClick(false, MainActivity.EventType.BOUND) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Stop")
                }

                Button(
                    onClick = { onClick(false, MainActivity.EventType.BOUND_DATA) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Get")
                }
            }
            if (dataState >= 0) {
                Text(
                    text = "Bound Service Data $dataState",
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(text = "Foreground Service")
            Row(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { onClick(true, MainActivity.EventType.FOREGROUND) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Start")
                }

                Button(
                    onClick = { onClick(false, MainActivity.EventType.FOREGROUND) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Stop")
                }
            }

            Text(text = "AIDL Service")
            Row(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { onClick(true, MainActivity.EventType.AIDL) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Start")
                }

                Button(
                    onClick = { onClick(false, MainActivity.EventType.AIDL) },
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(text = "Stop")
                }
            }
            if (aidlDataState.isNotEmpty()) {
                Text(
                    text = "AIDL Service Data $aidlDataState",
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ServiceSampleTheme {
        Services(dataState = -1, "") { _: Boolean, _: MainActivity.EventType ->

        }
    }
}