package com.example.invernaderoiot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothManagerHelper(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID base para conexiones SPP (Serial Port Profile) con HC-05
    private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(macAddress: String): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("BluetoothManager", "Bluetooth apagado o no disponible")
            return false
        }

        return try {
            val device = bluetoothAdapter!!.getRemoteDevice(macAddress)
            bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID)
            bluetoothAdapter!!.cancelDiscovery()
            bluetoothSocket!!.connect()
            outputStream = bluetoothSocket!!.outputStream
            Log.d("BluetoothManager", "Conectado al invernadero: $macAddress")
            true
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Error de conexión BT: ${e.message}")
            disconnect()
            false
        }
    }

    fun sendCommand(command: String) {
        if (outputStream == null) return

        try {
            outputStream!!.write(command.toByteArray())
            Log.d("BluetoothManager", "Comando enviado al Arduino: $command")
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Fallo al enviar comando: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Error al cerrar conexión: ${e.message}")
        }
    }
}