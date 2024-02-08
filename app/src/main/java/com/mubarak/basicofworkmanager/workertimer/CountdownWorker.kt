package com.mubarak.basicofworkmanager.workertimer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters


class CountdownWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val value = inputData.getLong("MAIN_TIMER_DURATION", 0)
        val intent = Intent(applicationContext, CountdownService::class.java)
        intent.putExtra("TIMER_DURATION", value)
        applicationContext.startService(intent)
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        Log.e("TAG", "onStopped: called")
        WorkManager.getInstance(applicationContext).cancelAllWork()
    }
}