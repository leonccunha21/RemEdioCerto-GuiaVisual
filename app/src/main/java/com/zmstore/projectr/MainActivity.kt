package com.zmstore.projectr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.navigation.NavGraph
import com.zmstore.projectr.ui.navigation.Screen
import com.zmstore.projectr.ui.theme.*
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
                        // Premium Drawer Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(MedicleanTeal, MedicleanTealDark)
                                    )
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Versão 2.4 - Premium",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MedicleanTeal.copy(alpha = 0.1f),
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            ) {
                androidx.compose.material3.Scaffold(
                    bottomBar = {
                        if (currentRoute == Screen.Home.route || currentRoute == Screen.MedicationList.route || currentRoute == Screen.History.route || currentRoute == Screen.Profile.route) {
                            Surface(
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                NavigationBar(
                                    containerColor = if (isSystemInDarkTheme()) Color(0xFF17201F) else Color.White,
                                    modifier = Modifier.height(80.dp),
                                    tonalElevation = 0.dp
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
                                            icon = { 
                                                Icon(
                                                    icon, 
                                                    contentDescription = label,
                                                    modifier = Modifier.size(26.dp)
                                                ) 
                                            },
                                            label = { 
                                                Text(
                                                    label, 
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ) 
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MedicleanTeal,
                                                selectedTextColor = MedicleanTeal,
                                                unselectedIconColor = MedicleanDarkGreen.copy(alpha = 0.4f),
                                                unselectedTextColor = MedicleanDarkGreen.copy(alpha = 0.4f),
                                                indicatorColor = MedicleanTeal.copy(alpha = 0.12f)
                                            )
                                        )
                                    }
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
