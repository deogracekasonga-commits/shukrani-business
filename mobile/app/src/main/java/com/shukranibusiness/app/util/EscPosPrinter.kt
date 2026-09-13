package com.shukranibusiness.app.util

import android.bluetooth.BluetoothAdapter
import java.util.UUID

/**
 * Impression texte brut sur imprimante thermique Bluetooth (protocole ESC/POS,
 * profil SPP — le standard des petites imprimantes de reçus bon marché).
 * Doit être appelé depuis un contexte hors du thread principal (I/O bloquant).
 */
object EscPosPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val INIT = byteArrayOf(0x1B, 0x40) // ESC @ : réinitialise l'imprimante
    private val CUT = byteArrayOf(0x1D, 0x56, 0x00) // GS V 0 : coupe le papier (si supportée)

    fun printLines(deviceAddress: String, lines: List<String>) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val device = adapter.getRemoteDevice(deviceAddress)

        adapter.cancelDiscovery()
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        socket.use {
            it.connect()
            val out = it.outputStream
            out.write(INIT)
            for (line in lines) {
                out.write(line.toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x0A))
            }
            out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            out.write(CUT)
            out.flush()
        }
    }
}
