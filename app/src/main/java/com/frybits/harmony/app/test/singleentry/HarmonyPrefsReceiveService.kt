package com.frybits.harmony.app.test.singleentry

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.frybits.harmony.app.ITERATIONS
import com.frybits.harmony.app.NUM_TESTS
import com.frybits.harmony.app.PREFS_NAME
import com.frybits.harmony.getHarmonySharedPreferences
import kotlin.system.measureTimeMillis

/**
 * Created by Pablo Baxter (Github: pablobaxter)
 */

class HarmonyPrefsReceiveService : Service() {

    private lateinit var harmonyActivityPrefs: SharedPreferences

    // This listener receives changes that occur to this shared preference from any process, not just this one.
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        val now = SystemClock.elapsedRealtime()
        require(prefs === harmonyActivityPrefs)
        val activityTestTime = prefs.getLong(key, -1L)
        if (activityTestTime > -1L) {
            timeCaptureList.add(now - activityTestTime)
        } else {
            Log.e("Trial", "${this::class.java.simpleName}: Got default long value!")
        }
    }

    private var isStarted = false
    private var isRegistered = false

    private val testKeyArray = Array(ITERATIONS) { i -> "test$i" }

    private val timeCaptureList = ArrayList<Long>(ITERATIONS * NUM_TESTS)
    private val singleReadTimeCaptureList = ArrayList<Long>(ITERATIONS * NUM_TESTS)
    private val totalReadTimeCaptureList = ArrayList<Long>(NUM_TESTS)

    override fun onCreate() {
        super.onCreate()
        harmonyActivityPrefs = getHarmonySharedPreferences(PREFS_NAME)
        timeCaptureList.clear()
        singleReadTimeCaptureList.clear()
        totalReadTimeCaptureList.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startCommand = intent?.getBooleanExtra("START", false) ?: false
        val endCommand = intent?.getBooleanExtra("STOP", false) ?: false
        if (!isStarted && startCommand) {
            Log.i("Trial", "${this::class.java.simpleName}: Starting service to receive from main process!")
            isStarted = true
            isRegistered = true
            harmonyActivityPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        }
        if (isStarted && endCommand) {
            Log.i("Trial", "${this::class.java.simpleName}: Stopping service to receive from main process!")
            val measure = measureTimeMillis {
                testKeyArray.forEach { s ->
                    val readTime = measureTimeMillis {
                        if (harmonyActivityPrefs.getLong(s, -1L) == -1L) {
                            Log.e("Trial", "${this::class.java.simpleName}: Key $s was not found!")
                        }
                    }
                    singleReadTimeCaptureList.add(readTime)
                }
            }
            totalReadTimeCaptureList.add(measure)
            isStarted = false
            harmonyActivityPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
            isRegistered = false
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Trial", this::class.java.simpleName)
        Log.i("Trial", "${this::class.java.simpleName}: =========================Harmony Single Read=========================")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Read count: ${singleReadTimeCaptureList.size}, expecting ${ITERATIONS * NUM_TESTS}")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Average to read one item: ${singleReadTimeCaptureList.average()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Max to read one item: ${singleReadTimeCaptureList.max()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Min to read one item: ${singleReadTimeCaptureList.min()} ms")

        Log.i("Trial", this::class.java.simpleName)
        Log.i("Trial", "${this::class.java.simpleName}: =========================Harmony Total Read=========================")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Read test count: ${totalReadTimeCaptureList.size}, expecting $NUM_TESTS")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Average read test time: ${totalReadTimeCaptureList.average()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Max read test time: ${totalReadTimeCaptureList.max()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Min read test time: ${totalReadTimeCaptureList.min()} ms")

        Log.i("Trial", this::class.java.simpleName)
        Log.i("Trial", "${this::class.java.simpleName}: =========================Harmony Total Receive=========================")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Receive Capture count: ${timeCaptureList.size}, expecting ${ITERATIONS * NUM_TESTS}")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Average time to receive from main process: ${timeCaptureList.average()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Max time to receive from main process: ${timeCaptureList.max()} ms")
        Log.i("Trial", "${this::class.java.simpleName}: Harmony - Min time to receive from main process: ${timeCaptureList.min()} ms")
    }
}
