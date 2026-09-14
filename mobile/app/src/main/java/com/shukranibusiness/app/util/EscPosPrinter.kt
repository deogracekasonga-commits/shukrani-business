package com.shukranibusiness.app.util

import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.util.UUID

/**
 * Impression sur imprimante thermique Bluetooth (protocole ESC/POS, profil SPP —
 * le standard des petites imprimantes de reçus bon marché). Doit être appelé
 * depuis un contexte hors du thread principal (I/O bloquant).
 */
object EscPosPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val INIT = byteArrayOf(0x1B, 0x40) // ESC @ : réinitialise l'imprimante
    private val CUT = byteArrayOf(0x1D, 0x56, 0x00) // GS V 0 : coupe le papier (si supportée)

    /** Largeur d'impression standard pour un rouleau 58mm à 203dpi. */
    private const val PRINTER_WIDTH_DOTS = 384

    /**
     * Imprime un reçu : logo (optionnel) puis lignes de texte, puis coupe le papier.
     * [logo] doit être fourni décodé (Bitmap), pas comme ressource — voir [ReceiptPrinter].
     */
    fun printReceipt(deviceAddress: String, logo: Bitmap?, lines: List<String>) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val device = adapter.getRemoteDevice(deviceAddress)

        adapter.cancelDiscovery()
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        socket.use {
            it.connect()
            val out = it.outputStream
            out.write(INIT)
            logo?.let { bitmap -> out.write(bitmapToEscPosRaster(bitmap)) }
            for (line in lines) {
                out.write(line.toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x0A))
            }
            out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            out.write(CUT)
            out.flush()
        }
    }

    /** Convertit un logo en commande raster ESC/POS (GS v 0), en noir et blanc, sur fond blanc. */
    private fun bitmapToEscPosRaster(source: Bitmap): ByteArray {
        // Compose sur fond blanc d'abord : un PNG avec transparence donnerait sinon des
        // pixels noirs (RVB à 0) là où il ne devrait rien y avoir d'imprimé.
        val onWhite = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(onWhite).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }

        val targetWidth = PRINTER_WIDTH_DOTS
        val targetHeight = (source.height * (targetWidth.toFloat() / source.width)).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(onWhite, targetWidth, targetHeight, true)

        val bytesPerRow = (targetWidth + 7) / 8
        val imageData = ByteArray(bytesPerRow * targetHeight)

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val pixel = scaled.getPixel(x, y)
                val luminance = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
                if (luminance < 128) {
                    val byteIndex = y * bytesPerRow + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    imageData[byteIndex] = (imageData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (bytesPerRow and 0xFF).toByte(), ((bytesPerRow shr 8) and 0xFF).toByte(),
            (targetHeight and 0xFF).toByte(), ((targetHeight shr 8) and 0xFF).toByte()
        )
        return header + imageData
    }
}
