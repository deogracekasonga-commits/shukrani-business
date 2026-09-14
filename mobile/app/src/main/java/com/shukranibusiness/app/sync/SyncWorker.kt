package com.shukranibusiness.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository

/**
 * Pousse vers Supabase toutes les ventes locales en attente de synchronisation. Ne fait rien si
 * cet appareil n'a pas encore été enregistré (voir Réglages > Synchronisation à distance).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val deviceToken = prefs.deviceToken
        val deviceId = prefs.deviceId
        if (deviceToken.isNullOrBlank() || deviceId.isNullOrBlank()) {
            return Result.success()
        }

        val repository = ShopRepository(applicationContext)
        val pending = repository.getPendingSyncSales()
        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true
        for (sale in pending) {
            val items = repository.getSaleItems(sale.id)
            val pushed = try {
                SupabaseSyncClient.pushSale(deviceToken, deviceId, prefs.shopName, sale, items)
            } catch (e: Exception) {
                false
            }
            if (pushed) {
                repository.markSynced(sale.id)
            } else {
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}
