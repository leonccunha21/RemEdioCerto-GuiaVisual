package com.zmstore.projectr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.navigation.NavGraph
import com.zmstore.projectr.ui.theme.ProjectRTheme
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Medication
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zmstore.projectr.ui.theme.*
import com.zmstore.projectr.ui.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch

import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.net.Uri

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) {}
        
        enableEdgeToEdge()
        setContent {
            ProjectRTheme {
                val userPrefs by viewModel.userPreferences.collectAsState()
                var isAuthenticated by remember { mutableStateOf(false) }

                LaunchedEffect(userPrefs.isBiometricEnabled) {
                    if (userPrefs.isBiometricEnabled && !isAuthenticated) {
                        showBiometricPrompt {
                            isAuthenticated = true
                        }
                    } else {
                        isAuthenticated = true
                    }
                }

                if (isAuthenticated) {
                    MainContent()
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MedicleanTeal)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Acesso Protegido", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = { showBiometricPrompt { isAuthenticated = true } }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Desbloquear")
                            }
                        }
                    }
                }
            }
        }
    }

    // Permitido chamar agora via NavGraph apenas após o login
    fun checkAlarmAndNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.fromParts("package", packageName, null)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Notifica o usuário ou solicita diretamente
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    @Composable
    private fun MainContent() {
        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        var showRationale by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult<String, Boolean>(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasCameraPermission = isGranted
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentRoute != Screen.Login.route,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(end = 56.dp)
                    ) {
                        Spacer(modifier = Modifier.padding(24.dp))
                        Text(stringResource(R.string.home_drawer_title), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        
                        val drawerItems = listOf(
                            Triple(stringResource(R.string.home_drawer_home), Icons.Default.Home, Screen.Home.route),
                            Triple("Meus Remédios", Icons.Default.Medication, Screen.MedicationList.route),
                            Triple("Alarmes e Alertas", Icons.Default.Notifications, Screen.Alarms.route),
                            Triple(stringResource(R.string.home_drawer_calendar), Icons.Default.CalendarToday, Screen.History.route),
                            Triple(stringResource(R.string.home_drawer_profile), Icons.Default.Person, Screen.Profile.route),
                            Triple(stringResource(R.string.home_drawer_help), Icons.Default.Info, Screen.Help.route)
                        )

                        drawerItems.forEach { (label, icon, route) ->
                            val selected = currentRoute == route
                            NavigationDrawerItem(
                                icon = { Icon(icon, contentDescription = null, tint = if(selected) MedicleanTeal else MaterialTheme.colorScheme.onSurface) },
                                label = { Text(label, color = if(selected) MedicleanTeal else MaterialTheme.colorScheme.onSurface, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal) },
                                selected = selected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            ) {
                androidx.compose.material3.Scaffold(
                    bottomBar = {
                        if (currentRoute == Screen.Home.route || currentRoute == Screen.MedicationList.route || currentRoute == Screen.History.route || currentRoute == Screen.Profile.route) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                val items = listOf(
                                    Triple("Início", Icons.Default.Home, Screen.Home.route),
                                    Triple("Remédios", Icons.Default.Medication, Screen.MedicationList.route),
                                    Triple("Histórico", Icons.Default.CalendarToday, Screen.History.route),
                                    Triple("Perfil", Icons.Default.Person, Screen.Profile.route)
                                )

                                items.forEach { (label, icon, route) ->
                                    val selected = currentRoute == route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MedicleanTeal,
                                            selectedTextColor = MedicleanTeal,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            indicatorColor = MedicleanMint.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController, 
                            viewModel = viewModel,
                            onCheckCameraPermission = {
                                if (hasCameraPermission) {
                                    true
                                } else {
                                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                                        showRationale = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    false
                                }
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onRequestAlarmPermission = { checkAlarmAndNotificationPermission() }
                        )
                    }
                }
            }
        }

        if (showRationale) {
            PermissionRationaleDialog(
                onConfirm = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onDismiss = { showRationale = false }
            )
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica")
            .setSubtitle("Acesse seus dados de saúde com segurança")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun PermissionRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_title)) },
        text = { Text(stringResource(R.string.permission_rationale)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.permission_btn_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.camera_btn_cancel))
            }
        }
    )
}
