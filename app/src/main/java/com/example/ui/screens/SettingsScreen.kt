/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: SettingsScreen.kt
 *
 * Commentary / Architectural Overview:
 * This screen provides the device and protocol configuration interface.
 * Users can customize device identity, view hardware Wi-Fi MAC / IP status, configure UI scaling,
 * review diagnostic logs, check runtime permissions, and inspect Shizuku privileged system integration.
 *
 * Visual Tokens & Theming:
 * - Implemented in the Cyberpunk Dark design system:
 *   - Background: CyberBackground (#0B0E14)
 *   - Surfaces: CyberSurface (#151A22) and CyberCard (#161D2A) with 1dp CyberBorder (#24324D)
 *   - Toggles & Inputs: CyberSwitch with cyan/emerald on-state and muted off-state.
 *   - Dialogs: Rounded CyberDialog with dark card background and accent border.
 */

package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.diagnostic.AppDiagnostics
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.BadgeType
import com.example.ui.components.DisplaySettingsCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import androidx.compose.foundation.BorderStroke
import com.example.ui.components.UiThemeSelectorCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current
    val localIp by viewModel.localIp.collectAsState()
    val shizukuStatus by viewModel.shizukuManager.status.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf(viewModel.deviceIdentity.deviceName) }

    // Counter used to trigger recomposition of permission checks on resume
    var permissionCheckTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionCheckTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionCheckTrigger++
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionCheckTrigger++
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = CyberCard,
            titleContentColor = CyberTextPrimary,
            textContentColor = CyberTextSecondary,
            title = {
                Text(
                    text = "EDIT DEVICE NAME",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = CyberCyan
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            viewModel.updateDeviceName(newNameInput.trim())
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("SAVE", color = CyberBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditNameDialog = false },
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text("CANCEL", color = CyberTextMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "Settings & Identity",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage cryptographic identity, permissions, and storage paths",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Received Files Storage Location Card
        item {
            GlassCard(borderColor = CyberCyan.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = CyberCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Received Files Destination",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "All incoming P2P files save directly to device downloads:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B14))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "/storage/emulated/0/Download/PeerLink/",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("/storage/emulated/0/Download/PeerLink/"))
                                    viewModel.postToast("Path copied")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Path", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ALL PERMISSION SWITCH TOGGLE STATUS CARD
        item {
            GlassCard(borderColor = ElectricViolet.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = ElectricViolet)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "System Permissions & Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Real-time toggle status of all app permissions",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(type = BadgeType.ENCRYPTED)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Camera Permission
                    PermissionSwitchRow(
                        title = "Camera Access",
                        description = "Required for live P2P video calling & QR scanning",
                        icon = Icons.Default.CameraAlt,
                        isGranted = hasPermission(context, Manifest.permission.CAMERA, permissionCheckTrigger),
                        onToggle = { enable ->
                            if (enable) {
                                singlePermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))

                    // 2. Microphone / Audio Permission
                    PermissionSwitchRow(
                        title = "Microphone (Audio)",
                        description = "Required for voice calling & audio streaming",
                        icon = Icons.Default.Mic,
                        isGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO, permissionCheckTrigger),
                        onToggle = { enable ->
                            if (enable) {
                                singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))

                    // 3. Nearby Wi-Fi Devices Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionSwitchRow(
                            title = "Nearby Wi-Fi Devices",
                            description = "Discovers & connects local peers without location",
                            icon = Icons.Default.Wifi,
                            isGranted = hasPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES, permissionCheckTrigger),
                            onToggle = { enable ->
                                if (enable) {
                                    singlePermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                                } else {
                                    openAppSettings(context)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))
                    }

                    // 4. Notifications Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionSwitchRow(
                            title = "Post Notifications",
                            description = "Displays incoming calls, file progress & service alerts",
                            icon = Icons.Default.Notifications,
                            isGranted = hasPermission(context, Manifest.permission.POST_NOTIFICATIONS, permissionCheckTrigger),
                            onToggle = { enable ->
                                if (enable) {
                                    singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openAppSettings(context)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))
                    }

                    // 5. Location Access (Fine & Coarse)
                    val isLocationGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION, permissionCheckTrigger) ||
                                            hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION, permissionCheckTrigger)
                    PermissionSwitchRow(
                        title = "Location Permission",
                        description = "Required for Wi-Fi Direct P2P scanning & subnet detection",
                        icon = Icons.Default.LocationOn,
                        isGranted = isLocationGranted,
                        onToggle = { enable ->
                            if (enable) {
                                multiplePermissionsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))

                    // 6. Bluetooth Devices
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PermissionSwitchRow(
                            title = "Nearby Bluetooth Devices",
                            description = "Scans & discovers peers via Bluetooth BLE beacons",
                            icon = Icons.Default.Bluetooth,
                            isGranted = hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT, permissionCheckTrigger) &&
                                        hasPermission(context, Manifest.permission.BLUETOOTH_SCAN, permissionCheckTrigger),
                            onToggle = { enable ->
                                if (enable) {
                                    singlePermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    openAppSettings(context)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkBorder.copy(alpha = 0.4f))
                    }

                    // 7. Storage Access
                    PermissionSwitchRow(
                        title = "Storage & Downloads Access",
                        description = "Direct writing to /Download/PeerLink/",
                        icon = Icons.Default.Folder,
                        isGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, permissionCheckTrigger)
                        } else {
                            true
                        },
                        onToggle = { enable ->
                            if (enable && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                singlePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { openAppSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Android App Permissions Settings")
                    }
                }
            }
        }

        // Device Profile Card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = viewModel.deviceIdentity.deviceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Broadcast name for discovery",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showEditNameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = CyberCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Device ID: ${viewModel.deviceIdentity.deviceId}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // App Interface Presentation Style (Default vs Glassmorphism)
        item {
            UiThemeSelectorCard(uiThemeManager = viewModel.uiThemeManager)
        }

        // Display & UI Resolution Compatibility Card
        item {
            DisplaySettingsCard(uiScaleManager = viewModel.uiScaleManager)
        }

        // Cryptography & Keys Card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Encryption",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Cipher: AES-256-GCM (Authenticated Encryption)\n" +
                               "• Key Exchange: Elliptic-Curve Diffie-Hellman (secp256r1)\n" +
                               "• Integrity: 128-bit GCM Auth Tag & SHA-256 Fingerprint\n" +
                               "• Replay Protection: Monotonic sequence tracking & 300s window\n" +
                               "• Zero External Telemetry: 100% offline peer-to-peer",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Local Key Fingerprint", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = viewModel.deviceIdentity.keyFingerprint,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(viewModel.deviceIdentity.keyFingerprint))
                            viewModel.postToast("Fingerprint copied")
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Fingerprint", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Video Call Auto-Recording Card
        item {
            val isAutoRecord by viewModel.callManager.callRecordingManager.isAutoRecordEnabled.collectAsState()
            GlassCard(
                borderColor = if (isAutoRecord) CrimsonError.copy(alpha = 0.5f) else DarkBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isAutoRecord) CrimsonError.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = if (isAutoRecord) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-Record Video Calls",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isAutoRecord) "Auto-record every answered call: ON" else "Auto-record: OFF",
                                    fontSize = 12.sp,
                                    color = if (isAutoRecord) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAutoRecord,
                            onCheckedChange = { viewModel.callManager.callRecordingManager.setAutoRecordEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrimsonError
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When enabled, every video call answered will automatically start recording and save directly to \"/Download/PeerLink/\" with in-call pause and resume controls.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Android WakeLock & Performance Card
        item {
            val isWakeLockActive by viewModel.wakeLockManager.isWakeLockActive.collectAsState()
            val isManualWakeLock by viewModel.wakeLockManager.manualOverride.collectAsState()

            GlassCard(
                borderColor = if (isWakeLockActive) NeonEmerald.copy(alpha = 0.5f) else DarkBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isWakeLockActive) NeonEmerald.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Power,
                                    contentDescription = null,
                                    tint = if (isWakeLockActive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Android WakeLock & Wi-Fi Lock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isWakeLockActive) "Continuous Active Mode (CAM): ACTIVE" else "Power Management: Standby",
                                    fontSize = 12.sp,
                                    color = if (isWakeLockActive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isManualWakeLock,
                            onCheckedChange = { viewModel.wakeLockManager.setManualWakeLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00363D),
                                checkedTrackColor = NeonEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Prevents Android OS from putting the CPU, Wi-Fi radio, and mesh sockets into low-power sleep mode during background transfers, active voice/video calls, screen sharing, and high-performance gaming.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Network Diagnostics Card
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transport Layer Diagnostics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Local Node Address: $localIp\n" +
                               "• TCP Server Port: ${viewModel.transportManager.serverPort} (Messaging, Screen & Pipelined 512KB Binary File Chunks)\n" +
                               "• UDP Datagram Port: 8990 (Low-latency Audio Streaming)\n" +
                               "• UDP Beacon Port: 8992 (Instant Subnet Broadcast)\n" +
                               "• Discovery Service: _peerlink._tcp (mDNS/DNS-SD)\n" +
                               "• Wi-Fi Direct: Enabled (Autonomous P2P Group)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Developer Diagnostics & Error Logs Card
        item {
            val logs by AppDiagnostics.logsFlow.collectAsState()
            GlassCard(borderColor = CyberCyan.copy(alpha = 0.6f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Developer Diagnostics & Crash Logs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "If you or another peer face auto-exit, crash, or one-sided calls, tap below to copy the full diagnosis report and stack trace to your clipboard to send to the developer.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            AppDiagnostics.copyReportToClipboard(
                                context,
                                mapOf(
                                    "Local IP" to localIp,
                                    "Shizuku Status" to viewModel.shizukuManager.status.value.name,
                                    "Call State" to (viewModel.callManager.callInfo.value?.callState?.name ?: "IDLE"),
                                    "WakeLock Active" to "${viewModel.wakeLockManager.isWakeLockActive.value}"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Complete Diagnostics to Clipboard",
                            color = Color(0xFF00363D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recent Log Tail (${logs.takeLast(4).size} entries):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        logs.takeLast(4).forEach { entry ->
                            Text(
                                text = entry,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) NeonEmerald.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) NeonEmerald else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGranted) "✓ Granted" else "✕ Denied",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) NeonEmerald else CrimsonError
                    )
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isGranted,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00363D),
                checkedTrackColor = NeonEmerald,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}

private fun hasPermission(context: Context, permission: String, trigger: Int): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
