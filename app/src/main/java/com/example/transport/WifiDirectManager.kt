// PeerLink Production Sync - Active
package com.example.transport

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import com.example.transport.model.PeerDevice
import com.example.transport.model.TransportType

class WifiDirectManager(
    private val context: Context,
    private val onPeersUpdated: (List<PeerDevice>) -> Unit,
    private val onConnected: (groupOwnerAddress: String, isGroupOwner: Boolean) -> Unit,
    private val onDisconnected: () -> Unit
) {
    private val tag = "WifiDirectManager"
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    var isP2pEnabled = false
        private set
    var isDiscovering = false
        private set

    var thisP2pDevice: WifiP2pDevice? = null
        private set

    init {
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (wifiP2pManager == null || channel == null) return

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        isP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        requestPeers()
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        requestConnectionInfo()
                    }
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                        val dev: WifiP2pDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                        }
                        dev?.let {
                            thisP2pDevice = it
                            Log.d(tag, "Local Wi-Fi P2P Device: ${it.deviceName} (${it.deviceAddress})")
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (wifiP2pManager == null || channel == null) return
        isDiscovering = true

        try {
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(tag, "Wi-Fi Direct peer discovery started")
                }

                override fun onFailure(reasonCode: Int) {
                    Log.w(tag, "Wi-Fi Direct peer discovery failed: $reasonCode")
                    isDiscovering = false
                }
            })
        } catch (e: Exception) {
            Log.w(tag, "Wi-Fi Direct discoverPeers exception: ${e.message}")
            isDiscovering = false
        }
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        if (wifiP2pManager == null || channel == null) return
        wifiP2pManager.requestPeers(channel) { peers: WifiP2pDeviceList ->
            val myDevice = thisP2pDevice
            val list = peers.deviceList
                .filter { device: WifiP2pDevice ->
                    // Never include own Wi-Fi P2P device
                    if (myDevice != null) {
                        if (device.deviceAddress.equals(myDevice.deviceAddress, ignoreCase = true)) return@filter false
                        if (device.deviceName.isNotBlank() && device.deviceName.equals(myDevice.deviceName, ignoreCase = true)) return@filter false
                    }
                    true
                }
                .map { device: WifiP2pDevice ->
                    PeerDevice(
                        id = device.deviceAddress,
                        name = device.deviceName.ifBlank { "Direct-Peer-${device.deviceAddress.takeLast(4)}" },
                        address = device.deviceAddress,
                        port = 8988,
                        transportType = TransportType.WIFI_DIRECT,
                        fingerprint = "P2P:" + device.deviceAddress.replace(":", "").take(8).uppercase()
                    )
                }
            onPeersUpdated(list)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        if (wifiP2pManager == null || channel == null) return
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(tag, "Initiated Wi-Fi Direct connection to $deviceAddress")
            }

            override fun onFailure(reason: Int) {
                Log.e(tag, "Wi-Fi Direct connection failed: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun createAutonomousGroup() {
        if (wifiP2pManager == null || channel == null) return
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(tag, "Wi-Fi Direct autonomous group created")
            }

            override fun onFailure(reason: Int) {
                Log.e(tag, "Failed to create Wi-Fi Direct group: $reason")
            }
        })
    }

    private fun requestConnectionInfo() {
        if (wifiP2pManager == null || channel == null) return
        wifiP2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo? ->
            if (info != null && info.groupFormed) {
                val groupOwnerAddress = info.groupOwnerAddress?.hostAddress ?: ""
                onConnected(groupOwnerAddress, info.isGroupOwner)
            } else {
                onDisconnected()
            }
        }
    }

    fun stop() {
        try {
            receiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering receiver: ${e.message}")
        } finally {
            receiver = null
        }
    }
}
