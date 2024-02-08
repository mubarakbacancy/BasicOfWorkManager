package com.mubarak.basicofworkmanager.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mubarak.basicofworkmanager.MainActivity
import com.mubarak.basicofworkmanager.MainActivity.Companion.KEY_WORK
import java.text.SimpleDateFormat
import java.util.Date

class SingleWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {

        return try {

            val counter = inputData.getInt(MainActivity.KEY_VALUE, 0)

            for (i in 0..counter) {
                Log.d("SingleWorker", "doWork: Single Work Running $i")
            }

            //output Data
            val time = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
            val currentDate = time.format(Date())
            val outPutDate = Data.Builder()
                .putString(KEY_WORK, "Finished time $currentDate")
                .build()

            Result.success(outPutDate)
        } catch (e: Exception) {
            Log.e("SingleWorker", "doWork: error $e")
            Result.failure()
        }

    }
}