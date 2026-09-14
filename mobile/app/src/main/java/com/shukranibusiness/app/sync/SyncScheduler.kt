package com.shukranibusiness.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Synchronisation "opportuniste" : on tente d'envoyer dès qu'il y a du réseau (immédiatement
 * après chaque vente/annulation), avec une tentative périodique en filet de sécurité pour
 * rattraper ce qui n'aurait pas pu partir pendant que l'app était fermée.
 */
object SyncScheduler {

    private const val IMMEDIATE_WORK_NAME = "shukra_pos_sync_immediate"
    private const val PERIODIC_WORK_NAME = "shukra_pos_sync_periodic"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** À appeler une fois au démarrage de l'app — idempotent (KEEP ne remplace pas l'existant). */
    fun schedulePeriodicSafetyNet(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
