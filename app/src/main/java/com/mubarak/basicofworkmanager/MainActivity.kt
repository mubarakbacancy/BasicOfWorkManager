package com.mubarak.basicofworkmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mubarak.basicofworkmanager.databinding.ActivityMainBinding
import com.mubarak.basicofworkmanager.worker.DoubleWorker
import com.mubarak.basicofworkmanager.worker.SingleWorker
import com.mubarak.basicofworkmanager.workertimer.CountdownService
import com.mubarak.basicofworkmanager.workertimer.CountdownWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_VALUE = "key_value"
        const val KEY_WORK = "KEY_WORK"
    }

    private lateinit var binding: ActivityMainBinding
    private val TIMER_DURATION: Long = 1 * 60 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        binding.apply {

            btnSingleWork.setOnClickListener {
                val workRequest =
                    OneTimeWorkRequestBuilder<SingleWorker>().setInitialDelay(3, TimeUnit.SECONDS)
                        .addTag("This is Single Work").setConstraints(constraints).build()
                WorkManager.getInstance(applicationContext).enqueue(workRequest)
            }

            //Chaining work
            btnParallelWorkers.setOnClickListener {
                val singleWorker = OneTimeWorkRequestBuilder<SingleWorker>().build()
                val doubleWorker = OneTimeWorkRequestBuilder<DoubleWorker>().build()

                val parallelWorker = mutableListOf<OneTimeWorkRequest>()
                parallelWorker.add(singleWorker)
                parallelWorker.add(doubleWorker)

                WorkManager.getInstance(this@MainActivity)
                    .beginWith(parallelWorker)
                    .enqueue()
                /*
                                WorkManager.getInstance(this@MainActivity)
                                    .beginWith(singleWorker)
                                    .then(doubleWorker)
                                    .enqueue()*/

            }

            btnWorkLiveData.setOnClickListener {

                val inputData = Data.Builder()
                    .putInt(KEY_VALUE, 9000)
                    .build()

                val singleWorker = OneTimeWorkRequestBuilder<SingleWorker>()
                    .setInputData(inputData)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(this@MainActivity).beginWith(singleWorker).enqueue()

                WorkManager.getInstance(this@MainActivity).getWorkInfoByIdLiveData(singleWorker.id)
                    .observe(this@MainActivity) { workInfo ->
                        binding.tvStatus.text = workInfo.state.name

                        if (workInfo.state.isFinished) {
                            val data = workInfo.outputData.getString(KEY_WORK)
                            Toast.makeText(applicationContext, data.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }


            btnPeriodicWork.setOnClickListener {

                val periodicWorker = PeriodicWorkRequestBuilder<DoubleWorker>(
                    15, // Minimum allowed interval is 15 minutes
                    TimeUnit.SECONDS
                ).build()

                WorkManager.getInstance(applicationContext).enqueue(periodicWorker)
            }

            btnStartTimer.setOnClickListener {
                // Create a Data object and add key-value pairs to it
                // 15 minutes in milliseconds
                val TIMER_DURATION = (15 * 60 * 1000).toLong()

                val inputData: Data = Data.Builder()
                    .putLong("MAIN_TIMER_DURATION", TIMER_DURATION)
                    .build()

                val workRequest = OneTimeWorkRequest.Builder(CountdownWorker::class.java)
                    .setInitialDelay(0, TimeUnit.SECONDS) // Delay before starting the worker
                    .setInputData(inputData)

                    .build()

                WorkManager.getInstance(applicationContext).enqueue(workRequest)
            }

            broadcastTimerDataHandling()
        }

    }

    private fun broadcastTimerDataHandling() {

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    binding.tvTimer.text = intent?.getStringExtra("remaining_time")
                }

            }, IntentFilter("countdown-timer")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        val stopIntent = Intent(this, CountdownService::class.java)
        stopIntent.action = CountdownService.ACTION_STOP_TIMER
        startService(stopIntent)
        Log.e("TAG", "onDestroy: MainActivity called")
    }
}