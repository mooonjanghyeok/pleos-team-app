package com.example.pleosteamapp

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * AAOS CarUxRestrictions observer.
 * UNKNOWN must not enable learning in Release builds.
 */
internal enum class KDriveUxState {
    UNKNOWN, PARKED, RESTRICTED
}

internal class KDriveUxMonitor(
    private val context: Context,
    private val onState: (KDriveUxState) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var active = false
    private var generation = 0

    fun start() {
        stop()
        active = true
        val ticket = generation

        if (!context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_AUTOMOTIVE
            )
        ) {
            Log.w("KDRIVE_UX", "NOT_AUTOMOTIVE; fail closed")
            return
        }

        try {
            val carInstance = Car.createCar(context)
                ?: throw IllegalStateException(
                    "Car.createCar returned null"
                )

            car = carInstance

            val service = carInstance.getCarManager(
                Car.CAR_UX_RESTRICTION_SERVICE
            ) as? CarUxRestrictionsManager
                ?: throw IllegalStateException(
                    "UX manager unavailable"
                )

            manager = service

            service.registerListener {
                restrictions: CarUxRestrictions ->

                handler.post {
                    if (active && generation == ticket) {
                        publish(restrictions)
                    }
                }
            }

            // Register first, then read the current state.
            publish(service.currentCarUxRestrictions)

        } catch (error: Exception) {
            Log.e("KDRIVE_UX", "Car UX unavailable", error)
            stop()

        } catch (error: LinkageError) {
            Log.e("KDRIVE_UX", "android.car missing", error)
            stop()
        }
    }

    private fun publish(ux: CarUxRestrictions?) {
        if (!active) return

        val state = when {
            ux == null -> KDriveUxState.UNKNOWN

            ux.isRequiresDistractionOptimization ->
                KDriveUxState.RESTRICTED

            else -> KDriveUxState.PARKED
        }

        onState(state)
    }

    fun stop() {
        generation++
        active = false

        try {
            manager?.unregisterListener()
        } catch (error: Exception) {
            Log.w(
                "KDRIVE_UX",
                "unregisterListener failed",
                error
            )
        }

        manager = null

        try {
            car?.disconnect()
        } catch (error: Exception) {
            Log.w(
                "KDRIVE_UX",
                "Car.disconnect failed",
                error
            )
        }

        car = null
        onState(KDriveUxState.UNKNOWN)
    }
}