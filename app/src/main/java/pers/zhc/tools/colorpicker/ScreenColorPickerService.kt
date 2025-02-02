package pers.zhc.tools.colorpicker

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import pers.zhc.tools.BaseService
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.androidAssert

class ScreenColorPickerService : BaseService() {
    private var mediaProjection: MediaProjection? = null

    private val receivers = object {
        lateinit var colorPickerOperation: ScreenColorPickerOperationReceiver
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForeground(
            System.currentTimeMillis().hashCode(),
            buildForegroundNotification()
        )

        receivers.colorPickerOperation = ScreenColorPickerOperationReceiver(this).also {
            applicationContext.registerReceiver(it, IntentFilter().apply {
                addAction(ScreenColorPickerOperationReceiver.ACTION_START)
                addAction(ScreenColorPickerOperationReceiver.ACTION_STOP)
            })
        }

        ScreenColorPickerMainActivity.serviceRunning = true
    }

    override fun onDestroy() {
        applicationContext.unregisterReceiver(receivers.colorPickerOperation)
        mediaProjection!!.stop()
        ScreenColorPickerMainActivity.serviceRunning = false
    }

    private fun buildForegroundNotification(): Notification {
        val builder = NotificationCompat.Builder(applicationContext, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
        return builder.apply {
            setSmallIcon(R.drawable.ic_db)
            setOngoing(true)
            setContentTitle(getString(R.string.screen_color_picker_running_notification_title))
        }.build()
    }

    private var startTimes = 0
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!
        // only start once
        androidAssert(startTimes++ == 0)
        val projectionData = intent.getParcelableExtra<Intent>(EXTRA_PROJECTION_DATA)!!

        val mpm = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(Activity.RESULT_OK, projectionData)

        applicationContext.sendBroadcast(Intent(ScreenColorPickerCheckpointReceiver.ACTION_SERVICE_STARTED).apply {
            putExtra(EXTRA_REQUEST_ID, intent.getStringExtra(EXTRA_REQUEST_ID))
        })

        return START_NOT_STICKY
    }

    fun start(requestId: String) {
        val colorPickerManager = ScreenColorPickerManager(this, requestId, mediaProjection!!)
        colorPickerManager.start()
        pickerManagersMap[requestId] = colorPickerManager
    }

    fun stop(requestId: String) {
        pickerManagersMap.remove(requestId)!!.stop()
        if (pickerManagersMap.isEmpty()) {
            // all color pickers are stopped, close the service
            stopSelf()
        }
    }

    companion object {
        val pickerManagersMap = HashMap<String, ScreenColorPickerManager>()

        const val EXTRA_PROJECTION_DATA = "projectionData"

        const val EXTRA_REQUEST_ID = "requestId"
    }
}