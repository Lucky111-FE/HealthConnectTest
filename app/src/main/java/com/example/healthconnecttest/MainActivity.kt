package com.example.healthconnecttest

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.example.healthconnecttest.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


private const val TAG = "MainActivity_싸피"
class MainActivity : AppCompatActivity() {
    private val PERMISSIONS =
        setOf(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )

//    private val All_Permissions = setOf(
//        Permissions.createReadPermission(StepsRecord::class)
//        Permissions.createReadPermission(ActiveCaloriesBurnedRecord::class)
//    )
    var realStepCounter = 0L
    var realCalrories = 0.toDouble()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val providerPackageName = "com.google.android.apps.healthdata"

        Log.d(TAG, "onCreate: Permission Controller 실행")
        // Create the permissions launcher
        val permissionController = healthConnectClient.permissionController

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        Log.d(TAG, "onCreate: Permission Controller 실행")

        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            Log.d(TAG, "onCreate: $granted")
            if (granted.containsAll(PERMISSIONS)) {
                // Permissions successfully granted
                Log.d(TAG, "onCreate: 권한 있음")
                lifecycleScope.launch {
                    Log.d(TAG, "onCreate: 스텝 카운터")

                    val zoneId = ZoneId.of("UTC")
                    val zonedDateTime: ZonedDateTime = ZonedDateTime.now()

                    Log.d(TAG, "onCreate: zone date time : $zonedDateTime")
                    val localDateTime = zonedDateTime.toLocalDateTime()
                    Log.d(TAG, "onCreate: LocalDateTime = $localDateTime")

                    val zeroTime = localDateTime.withHour(0).withMinute(0).withSecond(0)
                    val todayMidnight = zeroTime.atZone(zoneId).toInstant()

                    val nowInstant = localDateTime.atZone(zoneId).toInstant()

                    Log.d(TAG, "onCreate: 시작시간 $todayMidnight   종료시간 $nowInstant")

                    aggregateSteps(healthConnectClient, todayMidnight, nowInstant)
                    aggregateCalories(healthConnectClient, todayMidnight, nowInstant)
                    binding.step.text = realStepCounter.toString()
                    binding.calories.text = realCalrories.toString()
                }
            } else {
                Log.d(TAG, "onCreate: 권한 없음")

            }
        }
        requestPermissions.launch(PERMISSIONS)

        val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.d(TAG, "onCreate: unavailable")
            return // early return as there is no viable integration
        }
// Issue operations with healthConnectClient

        lifecycleScope.launch {
            Log.d(TAG, "onCreate: 스텝 카운터")

            val zoneId = ZoneId.of("UTC")
            val zonedDateTime: ZonedDateTime = ZonedDateTime.now()

            Log.d(TAG, "onCreate: zone date time : $zonedDateTime")
            val localDateTime = zonedDateTime.toLocalDateTime()
            Log.d(TAG, "onCreate: LocalDateTime = $localDateTime")

            val zeroTime = localDateTime.withHour(0).withMinute(0).withSecond(0)
            val todayMidnight = zeroTime.atZone(zoneId).toInstant()

            val nowInstant = localDateTime.atZone(zoneId).toInstant()

            Log.d(TAG, "onCreate: 시작시간 $todayMidnight   종료시간 $nowInstant")

            aggregateSteps(healthConnectClient, todayMidnight, nowInstant)
            aggregateCalories(healthConnectClient, todayMidnight, nowInstant)
            binding.step.text = realStepCounter.toString()
            binding.calories.text = realCalrories.toString()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }

    suspend fun aggregateSteps(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // The result may be null if no data is available in the time range
            realStepCounter = response[StepsRecord.COUNT_TOTAL] ?: 0
            Log.d(TAG, "aggregateSteps: stepCounter $realStepCounter")
        } catch (e: Exception) {
            // Run error handling here
            Log.d(TAG, "aggregateSteps: error")
            Log.d(TAG, "aggregateSteps: $e")
        }
    }

    suspend fun aggregateCalories(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // The result may be null if no data is available in the time range
            response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let{
                Log.d(TAG, "aggregateCalories: ${it.inCalories}")
                realCalrories = it.inCalories}
            Log.d(TAG, "aggregateCalories: calories $realCalrories")
        } catch (e: Exception) {
            // Run error handling here
            Log.d(TAG, "aggregateCalories: error")
            Log.d(TAG, "aggregateCalories: $e")
        }
    }


}