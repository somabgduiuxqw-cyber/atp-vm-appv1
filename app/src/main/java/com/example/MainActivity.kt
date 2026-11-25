package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AtpViewModel
import com.example.ui.navigation.AtpRoutes
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppImporterScreen
import com.example.ui.screens.EnvironmentSettingsScreen
import com.example.ui.screens.GoogleServicesSettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportedAppsScreen
import com.example.ui.screens.StorageManagementScreen
import com.example.ui.theme.AtpVmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtpVmTheme {
                AtpVmApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtpVmApp(
    viewModel: AtpViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AtpRoutes.HOME
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeStatusMessage()
    }

    val topLevelRoutes = setOf(
        AtpRoutes.HOME,
        AtpRoutes.IMPORTED_APPS,
        AtpRoutes.STORAGE,
        AtpRoutes.ENV_SETTINGS
    )

    val screenTitle = when (currentRoute) {
        AtpRoutes.HOME -> stringResource(R.string.app_name)
        AtpRoutes.IMPORTED_APPS -> stringResource(R.string.imported_title)
        AtpRoutes.APP_IMPORTER -> stringResource(R.string.importer_title)
        AtpRoutes.ENV_SETTINGS -> stringResource(R.string.env_title)
        AtpRoutes.GMS_SETTINGS -> stringResource(R.string.gms_title)
        AtpRoutes.STORAGE -> stringResource(R.string.storage_title)
        AtpRoutes.ABOUT -> stringResource(R.string.about_title)
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    if (currentRoute !in topLevelRoutes) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != AtpRoutes.ABOUT) {
                        IconButton(onClick = {
                            navController.navigate(AtpRoutes.ABOUT) { launchSingleTop = true }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.about_title)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == AtpRoutes.HOME,
                    onClick = {
                        navController.navigate(AtpRoutes.HOME) {
                            popUpTo(AtpRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = currentRoute == AtpRoutes.IMPORTED_APPS || currentRoute == AtpRoutes.APP_IMPORTER,
                    onClick = {
                        navController.navigate(AtpRoutes.IMPORTED_APPS) {
                            popUpTo(AtpRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_container),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_apps)) }
                )
                NavigationBarItem(
                    selected = currentRoute == AtpRoutes.STORAGE,
                    onClick = {
                        navController.navigate(AtpRoutes.STORAGE) {
                            popUpTo(AtpRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_storage),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_storage)) }
                )
                NavigationBarItem(
                    selected = currentRoute == AtpRoutes.ENV_SETTINGS || currentRoute == AtpRoutes.GMS_SETTINGS,
                    onClick = {
                        navController.navigate(AtpRoutes.ENV_SETTINGS) {
                            popUpTo(AtpRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AtpRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AtpRoutes.HOME) {
                HomeScreen(
                    uiState = uiState,
                    loadIcon = viewModel::loadIconBitmap,
                    onOpenImporter = { navController.navigate(AtpRoutes.APP_IMPORTER) },
                    onOpenImportedApps = { navController.navigate(AtpRoutes.IMPORTED_APPS) },
                    onOpenEnvSettings = { navController.navigate(AtpRoutes.ENV_SETTINGS) },
                    onOpenGmsSettings = { navController.navigate(AtpRoutes.GMS_SETTINGS) },
                    onOpenStorage = { navController.navigate(AtpRoutes.STORAGE) },
                    onOpenAbout = { navController.navigate(AtpRoutes.ABOUT) }
                )
            }
            composable(AtpRoutes.IMPORTED_APPS) {
                ImportedAppsScreen(
                    uiState = uiState,
                    loadIcon = viewModel::loadIconBitmap,
                    getHostLaunchIntent = viewModel::getHostLaunchIntent,
                    onOpenImporter = { navController.navigate(AtpRoutes.APP_IMPORTER) },
                    onVerifyContainer = viewModel::verifyContainer,
                    onClearVerification = viewModel::clearVerificationReport,
                    onWipeData = viewModel::wipeAppData,
                    onRemoveApp = viewModel::removeVirtualApp,
                    onTogglePerAppRoot = viewModel::setPerAppVirtualRoot,
                    onTogglePerAppGms = viewModel::setPerAppGmsBridge
                )
            }
            composable(AtpRoutes.APP_IMPORTER) {
                AppImporterScreen(
                    uiState = uiState,
                    loadIcon = viewModel::loadIconBitmap,
                    onScanHostApps = viewModel::scanHostApps,
                    onImportHostApp = viewModel::importHostApp,
                    onImportExternalApk = viewModel::importExternalApk,
                    onToggleSystemApps = viewModel::setIncludeSystemAppsInScanner
                )
            }
            composable(AtpRoutes.ENV_SETTINGS) {
                EnvironmentSettingsScreen(
                    uiState = uiState,
                    onSetIsolateAppData = viewModel::setIsolateAppData,
                    onSetIsolateExternalStorage = viewModel::setIsolateExternalStorage,
                    onSetVirtualRootPolicy = viewModel::setVirtualRootPolicy,
                    onSetPreferredAbi = viewModel::setPreferredAbi,
                    onOpenGmsSettings = { navController.navigate(AtpRoutes.GMS_SETTINGS) }
                )
            }
            composable(AtpRoutes.GMS_SETTINGS) {
                GoogleServicesSettingsScreen(
                    uiState = uiState,
                    onSetGmsMode = viewModel::setGmsMode,
                    onRefreshGmsCheck = viewModel::refreshDiagnosticsAndGms
                )
            }
            composable(AtpRoutes.STORAGE) {
                StorageManagementScreen(
                    uiState = uiState,
                    onClearAppCache = viewModel::clearAppCache,
                    onWipeAppData = viewModel::wipeAppData,
                    onRemoveApp = viewModel::removeVirtualApp,
                    onClearAllCaches = viewModel::clearAllCaches
                )
            }
            composable(AtpRoutes.ABOUT) {
                AboutScreen()
            }
        }
    }
}
