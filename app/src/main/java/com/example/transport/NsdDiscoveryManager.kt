package com.example.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.example.transport.model.PeerDevice
import com.example.transport.model.TransportType
import java.net.InetAddress

class NsdDiscoveryManager(
    private val context: Context,
    private val onDeviceFound: (PeerDevice) -> Unit,
    private val onDeviceLost: (String) -> Unit
) {
    private val tag = "NsdDiscoveryManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val serviceType = "_peerlink._tcp."

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    var isAdvertising = false
        private set
    var isDiscovering = false
        private set

    fun startAdvertising(
        deviceId: String,
        deviceName: String,
        port: Int = 8988,
        fingerprint: String = ""
    ) {
        if (nsdManager == null || isAdvertising) return

        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "PL-$deviceId"
                serviceType = this@NsdDiscoveryManager.serviceType
                setPort(port)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAttribute("id", deviceId)
                    setAttribute("name", deviceName)
                    setAttribute("fp", fingerprint)
                }
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(service: NsdServiceInfo) {
                    Log.d(tag, "Service registered: ${service.serviceName}")
                    isAdvertising = true
                }

                override fun onRegistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.e(tag, "Service registration failed: $errorCode")
                    isAdvertising = false
                }

                override fun onServiceUnregistered(service: NsdServiceInfo) {
                    Log.d(tag, "Service unregistered")
                    isAdvertising = false
                }

                override fun onUnregistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.e(tag, "Service unregistration failed: $errorCode")
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start NSD advertising: ${e.message}")
        }
    }

    fun stopAdvertising() {
        if (nsdManager == null || !isAdvertising) return
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering NSD service: ${e.message}")
        } finally {
            registrationListener = null
            isAdvertising = false
        }
    }

    fun startDiscovery() {
        if (nsdManager == null || isDiscovering) return

        try {
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d(tag, "NSD Discovery started")
                    isDiscovering = true
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d(tag, "Service found: ${service.serviceName}")
                    if (service.serviceType.contains("peerlink")) {
                        resolveService(service)
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.d(tag, "Service lost: ${service.serviceName}")
                    val id = service.serviceName.removePrefix("PL-")
                    onDeviceLost(id)
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(tag, "NSD Discovery stopped")
                    isDiscovering = false
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(tag, "Start discovery failed: $errorCode")
                    isDiscovering = false
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(tag, "Stop discovery failed: $errorCode")
                }
            }

            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start NSD discovery: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        try {
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.w(tag, "Resolve failed for ${service.serviceName}: $errorCode")
                }

                override fun onServiceResolved(service: NsdServiceInfo) {
                    val host: InetAddress = service.host ?: return
                    val hostAddress = host.hostAddress ?: return
                    val port = service.port

                    var id = service.serviceName.removePrefix("PL-")
                    var name = service.serviceName
                    var fp = ""

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        service.attributes?.let { attrs ->
                            attrs["id"]?.let { id = String(it) }
                            attrs["name"]?.let { name = String(it) }
                            attrs["fp"]?.let { fp = String(it) }
                        }
                    }

                    val peerDevice = PeerDevice(
                        id = id,
                        name = name,
                        address = hostAddress,
                        port = port,
                        transportType = TransportType.WIFI_NSD,
                        fingerprint = fp
                    )
                    onDeviceFound(peerDevice)
                }
            }
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(tag, "Error triggering service resolve: ${e.message}")
        }
    }

    fun stopDiscovery() {
        if (nsdManager == null || !isDiscovering) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery: ${e.message}")
        } finally {
            discoveryListener = null
            isDiscovering = false
        }
    }
}
