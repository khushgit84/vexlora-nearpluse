package com.vexlora.nearpulse

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.vexlora.nearpulse.ui.screens.*
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Sensors)
    object Radar : Screen("radar", "Radar", Icons.Default.Radar)
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Sos : Screen("sos", "SOS", Icons.Default.Campaign)
    object Circle : Screen("circle", "Circle", Icons.Default.Group)
    object Settings : Screen("settings", "Settings", Icons.Default.Tune)
}

class MainActivity : ComponentActivity() {
    private val viewModel: NearPulseViewModel by viewModels()
    private val pendingRoute = mutableStateOf<String?>(null)

    private val btEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.startAutoPulse()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkBluetoothAndStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        checkAndRequestPermissions()

        setContent {
            VexloraNearPulseTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

                val items = listOf(
                    Screen.Home,
                    Screen.Radar,
                    Screen.Chat,
                    Screen.Sos,
                    Screen.Circle,
                    Screen.Settings
                )

                // Handle incoming navigation intent from heads-up message or SOS notifications
                val target = pendingRoute.value
                LaunchedEffect(target) {
                    if (target != null) {
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        pendingRoute.value = null
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = CyberCard,
                            tonalElevation = 8.dp
                        ) {
                            items.forEach { screen ->
                                val selected = currentRoute == screen.route
                                val isSos = screen == Screen.Sos

                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            tint = when {
                                                isSos && selected -> CrimsonRed
                                                isSos -> CrimsonRed.copy(alpha = 0.7f)
                                                selected -> ElectricCyan
                                                else -> TextMuted
                                            }
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            color = when {
                                                isSos && selected -> CrimsonRed
                                                selected -> ElectricCyan
                                                else -> TextMuted
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = if (isSos) CrimsonRed.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberBgDark)
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToRadar = { navController.navigate(Screen.Radar.route) },
                                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                                    onNavigateToSos = { navController.navigate(Screen.Sos.route) },
                                    onNavigateToCircle = { navController.navigate(Screen.Circle.route) }
                                )
                            }
                            composable(Screen.Radar.route) {
                                RadarScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                                )
                            }
                            composable(Screen.Chat.route) {
                                ChatScreen(
                                    viewModel = viewModel,
                                    onNavigateToRadar = { navController.navigate(Screen.Radar.route) }
                                )
                            }
                            composable(Screen.Sos.route) {
                                SosScreen(viewModel = viewModel)
                            }
                            composable(Screen.Circle.route) {
                                SafetyCircleScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra("EXTRA_NAV_ROUTE")
        val peerAddress = intent?.getStringExtra("EXTRA_PEER_ADDRESS")
        val peerPseudonym = intent?.getStringExtra("EXTRA_PEER_PSEUDONYM")

        val testAction = intent?.getStringExtra("EXTRA_TEST_ACTION")
        if (testAction == "NOTIFICATION") {
            intent.removeExtra("EXTRA_TEST_ACTION")
            val delaySec = intent.getLongExtra("EXTRA_DELAY_SEC", 0L)
            viewModel.testMessageNotification(delaySec)
        }

        if (peerAddress != null) {
            intent?.removeExtra("EXTRA_PEER_ADDRESS")
            viewModel.selectPeerByAddress(peerAddress, peerPseudonym)
        }

        if (route != null) {
            intent?.removeExtra("EXTRA_NAV_ROUTE")
            pendingRoute.value = when (route) {
                "chat" -> Screen.Chat.route
                "sos" -> Screen.Sos.route
                "radar" -> Screen.Radar.route
                "settings" -> Screen.Settings.route
                "circle" -> Screen.Circle.route
                else -> Screen.Home.route
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBluetoothAndStart()
    }

    private fun checkBluetoothAndStart() {
        if (!viewModel.isBluetoothEnabled()) {
            try {
                btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (_: Exception) {}
        } else {
            viewModel.startAutoPulse()
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
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBluetoothAndStart()
        }
    }
}