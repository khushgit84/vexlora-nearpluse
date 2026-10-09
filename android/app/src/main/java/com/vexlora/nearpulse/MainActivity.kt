package com.vexlora.nearpulse

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vexlora.nearpulse.ui.screens.*
import com.vexlora.nearpulse.ui.theme.CyberBgDark
import com.vexlora.nearpulse.ui.theme.CyberBorder
import com.vexlora.nearpulse.ui.theme.ElectricCyan
import com.vexlora.nearpulse.ui.theme.TextMuted
import com.vexlora.nearpulse.ui.theme.VexloraNearPulseTheme
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Radar : Screen("radar", "Radar", Icons.Default.Radar)
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Sos : Screen("sos", "SOS", Icons.Default.Warning)
    object Circle : Screen("circle", "Circle", Icons.Default.GroupWork)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: NearPulseViewModel by viewModels {
        val app = application as NearPulseApplication
        NearPulseViewModel.Factory(app.repository)
    }

    private val requestBluetoothPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "NearPulse requires Bluetooth permissions to discover offline peers.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, "Bluetooth must be enabled for offline discovery", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            VexloraNearPulseTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Status Toast Collector
                LaunchedEffect(Unit) {
                    viewModel.statusNotice.collectLatest { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                val bottomNavItems = listOf(
                    Screen.Home,
                    Screen.Radar,
                    Screen.Chat,
                    Screen.Sos,
                    Screen.Circle
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = CyberBgDark,
                            tonalElevation = NavigationBarDefaults.Elevation
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            tint = if (selected) ElectricCyan else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            color = if (selected) ElectricCyan else TextMuted
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToRadar = { navController.navigate(Screen.Radar.route) },
                                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                                onNavigateToSos = { navController.navigate(Screen.Sos.route) },
                                onNavigateToCircle = { navController.navigate(Screen.Circle.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                        composable(Screen.Radar.route) {
                            RadarScreen(
                                viewModel = viewModel,
                                onPeerSelected = { peer ->
                                    viewModel.selectPeerForChat(peer)
                                    navController.navigate(Screen.Chat.route)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Chat.route) {
                            ChatScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Sos.route) {
                            SosScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Circle.route) {
                            SafetyCircleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Check if Bluetooth is turned on
        if (!viewModel.bleManager.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                enableBluetoothLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }
}
