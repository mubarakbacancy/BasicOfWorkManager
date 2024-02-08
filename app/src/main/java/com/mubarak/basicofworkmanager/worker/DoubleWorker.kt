package com.mubarak.basicofworkmanager.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class DoubleWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {


    override fun doWork(): Result {
        return try {
            for (i in 1..10) {
                Log.d("DoubleWorker", "doWork: DoubleWorker Work Running $i")
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}