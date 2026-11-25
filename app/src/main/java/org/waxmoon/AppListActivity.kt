package org.waxmoon

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
import com.hack.opensdk.BuildConfig as OpenSdkBuildConfig
import com.hack.utils.FileUtils
import org.waxmoon.ui.theme.GithubMultiAppTheme
import java.io.File

class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkBlue = Color.parseColor("#071426")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkBlue),
            navigationBarStyle = SystemBarStyle.dark(darkBlue)
        )
        setContent {
            GithubMultiAppTheme {
                AtpVirtualEnvironmentApp()
            }
        }
    }
}

private val assistInstallRequestContract = object : ActivityResultContract<String, String>() {
    var targetPackage: String = ""

    override fun createIntent(context: Context, input: String): Intent {
        targetPackage = input
        val file = File(context.externalCacheDir, "assist.apk")
        FileUtils.extractAsset(context, "assist.apk", file)
        val authority = "${context.packageName}:${MoonProvider::class.qualifiedName}"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String = targetPackage
}

private fun isAssistInstalled(context: Context): Boolean {
    return runCatching {
        context.packageManager.getPackageInfo(OpenSdkBuildConfig.ASSIST_PACKAGE, 0)
    }.isSuccess
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtpVirtualEnvironmentApp(
    viewModel: AtpViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AtpRoutes.HOME
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    val assistLauncher = rememberLauncherForActivityResult(contract = assistInstallRequestContract) { pkg ->
        if (!isAssistInstalled(context)) {
            Toast.makeText(context, R.string.toast_assist_missing, Toast.LENGTH_SHORT).show()
        } else if (pkg.isNotEmpty()) {
            viewModel.launchVirtualApp(pkg)
        }
    }

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

    if (uiState.assistPromptPackage != null) {
        val pendingPkg = uiState.assistPromptPackage!!
        AlertDialog(
            onDismissRequest = viewModel::dismissAssistPrompt,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("32-bit Assist Package Required") },
            text = {
                Text(
                    "This application uses 32-bit libraries and requires installing the ATP Assist package (${OpenSdkBuildConfig.ASSIST_PACKAGE}).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAssistPrompt()
                        assistLauncher.launch(pendingPkg)
                    }
                ) {
                    Text("Install Assist")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAssistPrompt) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    if (currentRoute !in topLevelRoutes) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    // MultiApp multi-space selector preserved in TopAppBar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = viewModel::decrementUserSpace,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Space",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${stringResource(R.string.text_space)}${uiState.userSpace}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = viewModel::incrementUserSpace,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Space",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (currentRoute != AtpRoutes.ABOUT) {
                        IconButton(onClick = {
                            navController.navigate(AtpRoutes.ABOUT) { launchSingleTop = true }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.about_title),
                                tint = MaterialTheme.colorScheme.primary
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.secondary
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationBarItem(
                    selected = currentRoute == AtpRoutes.HOME,
                    onClick = {
                        navController.navigate(AtpRoutes.HOME) {
                            popUpTo(AtpRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = itemColors,
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
                    colors = itemColors,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_container),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
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
                    colors = itemColors,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_storage),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
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
                    colors = itemColors,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AtpRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(160)) }
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
                    onLaunchVirtualApp = viewModel::launchVirtualApp,
                    onClearLaunchNotice = viewModel::clearLaunchNotice,
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
                    onRefreshGmsCheck = viewModel::refreshDiagnosticsAndGms,
                    onStageHostGms = viewModel::stageGoogleServicesFromHost
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