package com.mubarak.basicofworkmanager.workertimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.mubarak.basicofworkmanager.MainActivity
import com.mubarak.basicofworkmanager.R


class CountdownService : Service() {
    private var countDownTimer: CountDownTimer? = null
    private var isCountdownPaused = false

    //   private static long TIMER_DURATION = 15 * 60 * 1000; // 15 minutes in milliseconds
    private var TIMER_DURATION: Long = 0 // 15 minutes in milliseconds
    override fun onCreate() {
        super.onCreate()
    }

    private fun broadcastTimerData(time: String) {
        val intent = Intent("countdown-timer")
        intent.putExtra("remaining_time", time)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000).toInt() % 60
        val minutes = (millis / (1000 * 60) % 60).toInt()
        val hours = (millis / (1000 * 60 * 60) % 24).toInt()

//        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            // Use the received value
            remainingTime = intent.getLongExtra("TIMER_DURATION", 0)
        }
        createNotificationChannel()
        val notification = createNotification(formatTime(remainingTime), true).build()
        startForeground(NOTIFICATION_ID, notification)
        if (intent != null) {
            val action = intent.action
            if (ACTION_RESUME == action) {
                resumeCountdown()
                Log.e("TAG", "onStartCommand: ACTION_RESUME")
            } else if (ACTION_CANCEL == action) {
                cancelCountdown()
                Log.e("TAG", "onStartCommand: ACTION_CANCEL")
            } else if (ACTION_PAUSE == action) {
                pauseCountdown(remainingTime)
                Log.e("TAG", "onStartCommand: ACTION_PAUSE")
            } else if (ACTION_STOP_TIMER == action) {
                cancelCountdown()
                return START_NOT_STICKY
            } else {
                Log.e("TAG", "onStartCommand: ACTION_Start inner")
                startCountdown()
            }
        } else {
            startCountdown()
            Log.e("TAG", "onStartCommand: ACTION_Start")
        }
        return START_STICKY
    }

    fun cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
            stopForeground(true)
            stopSelf()
            WorkManager.getInstance(this).cancelAllWork()
        }
    }

    private fun pauseCountdown(newRemainingTime: Long) {
        if (!isCountdownPaused) {
            countDownTimer!!.cancel()
            isCountdownPaused = true
            updateNotification("Paused: " + formatTime(TIMER_DURATION), true)
        }
    }

    private fun resumeCountdown() {
        if (isCountdownPaused) {
            countDownTimer = object : CountDownTimer(TIMER_DURATION, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    updateNotification(formatTime(millisUntilFinished), true)
                    TIMER_DURATION = millisUntilFinished

                    // Broadcast remaining time to MainActivity
                    broadcastTimerData(formatTime(millisUntilFinished))
                }

                override fun onFinish() {
                    Toast.makeText(applicationContext, "Countdown finished", Toast.LENGTH_SHORT)
                        .show()
                    stopForeground(true)
                    stopSelf()
                    WorkManager.getInstance(applicationContext).cancelAllWork()
                }
            }
            countDownTimer?.start()
            isCountdownPaused = false
            updateNotification("Resumed", true)
        }
    }

    private fun startCountdown() {
        Log.e("TAG", "startCountdown: remainingTime : mubark start" + remainingTime)
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification(formatTime(millisUntilFinished), true)
                Log.e(
                    "CountdownService",
                    "onTick: millisUntilFinished " + formatTime(millisUntilFinished)
                )
                //                remainingTime = millisUntilFinished;
                TIMER_DURATION = millisUntilFinished


                // Broadcast remaining time to MainActivity
                broadcastTimerData(formatTime(millisUntilFinished))
            }

            override fun onFinish() {
                Toast.makeText(applicationContext, "Countdown finished", Toast.LENGTH_SHORT).show()
                stopForeground(true)
                stopSelf()
                WorkManager.getInstance(applicationContext).cancelAllWork()
            }
        }
        countDownTimer?.start()
    }

    override fun onDestroy() {
//        countDownTimer.cancel();
        super.onDestroy()
        Log.e("mubarak", "onDestroy: called")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun updateNotification(contentText: String, showActions: Boolean) {
        val builder = createNotification(contentText, showActions)
        val notification = builder.build()
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(
        contentText: String, showActions: Boolean
    ): NotificationCompat.Builder {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = Intent(this, NotificationActionReceiver::class.java)
        pauseIntent.action = ACTION_PAUSE
        val pausePendingIntent = PendingIntent.getBroadcast(
            this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelIntent = Intent(this, NotificationActionReceiver::class.java)
        cancelIntent.action = ACTION_CANCEL
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val resumeIntent = Intent(this, NotificationActionReceiver::class.java)
        resumeIntent.action = ACTION_RESUME
        val resumePendingIntent = PendingIntent.getBroadcast(
            this, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "MyChannelId")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Countdown Service")
            .setContentText("Time remaining: $contentText").setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true)
        if (showActions) {
            builder.addAction(R.drawable.ic_cancel, "Cancel", cancelPendingIntent)
            if (!isCountdownPaused) {
                builder.addAction(R.drawable.ic_cancel, "Pause", pausePendingIntent)
            } else {
                builder.addAction(R.drawable.ic_cancel, "Resume", resumePendingIntent)
            }
        }
        return builder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MyChannelId", "Your Channel Name", NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Your Channel Description"
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val ACTION_CANCEL = "com.mubarak.basicofworkmanager.workertimer.ACTION_CANCEL"
        private const val ACTION_RESUME = "com.mubarak.basicofworkmanager.workertimer.ACTION_RESUME"
        private const val ACTION_START = "com.mubarak.basicofworkmanager.workertimer.ACTION_START"
        private const val ACTION_PAUSE = "com.mubarak.basicofworkmanager.workertimer.ACTION_PAUSE"
        const val ACTION_STOP_TIMER = "com.mubarak.basicofworkmanager.workertimer.ACTION_STOP_TIMER"
        var remainingTime: Long = 0 // Store remaining time when paused
    }
}
