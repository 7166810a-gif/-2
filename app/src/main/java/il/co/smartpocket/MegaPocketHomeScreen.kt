@file:OptIn(ExperimentalMaterial3Api::class)

package il.co.smartpocket

import android.net.Uri
import android.content.Intent
import android.media.MediaPlayer
import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.co.smartpocket.core.catalog.MegaModule
import il.co.smartpocket.core.catalog.MegaModuleCatalog
import il.co.smartpocket.core.catalog.ModuleStage
import il.co.smartpocket.core.storage.LocalAccessRepository
import il.co.smartpocket.core.storage.LocalFileEntry

private val MegaBackground = Color(0xFF080F1C)
private val MegaSurface = Color(0xFF111D2E)
private val MegaSurfaceAlt = Color(0xFF17263A)
private val MegaCyan = Color(0xFF42D9F5)
private val MegaPurple = Color(0xFF9B8CFF)
private val MegaText = Color(0xFFF2F6FF)
private val MegaMuted = Color(0xFF9BAAC0)

@Composable
fun MegaPocketXApp() {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("mega_pocket_x_settings", android.content.Context.MODE_PRIVATE) }
    var activeTab by remember {
        mutableStateOf(settingsPrefs.getString("start_screen", "home")?.takeIf { it in setOf("home", "modules", "settings", "about") } ?: "home")
    }
    var compactCards by remember { mutableStateOf(settingsPrefs.getBoolean("compact_cards", false)) }
    var hidePlannedModules by remember { mutableStateOf(settingsPrefs.getBoolean("hide_planned", false)) }
    var showQuickActions by remember { mutableStateOf(settingsPrefs.getBoolean("quick_actions", true)) }
    var keepScreenOn by remember { mutableStateOf(settingsPrefs.getBoolean("keep_screen_on", false)) }
    var sortStorageBySize by remember { mutableStateOf(settingsPrefs.getBoolean("sort_storage_size", false)) }
    var accentName by remember { mutableStateOf(settingsPrefs.getString("accent", "cyan") ?: "cyan") }
    var textScale by remember { mutableStateOf(settingsPrefs.getFloat("text_scale", 1.0f).coerceIn(0.85f, 1.25f)) }
    val accentColor = when (accentName) {
        "violet" -> Color(0xFFB39DFF)
        "emerald" -> Color(0xFF55D6A5)
        "amber" -> Color(0xFFFFC66D)
        else -> MegaCyan
    }
    val scaledDensity = androidx.compose.ui.unit.Density(LocalDensity.current.density, textScale)
    var selectedModuleId by remember { mutableStateOf<String?>(null) }
    var showLegacyTools by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var modelName by remember {
        mutableStateOf(LocalAccessRepository.readUri(context, "local_model_uri")
            ?.let { LocalAccessRepository.displayName(context, it) })
    }
    var storageName by remember {
        mutableStateOf(LocalAccessRepository.readUri(context, "storage_tree_uri")
            ?.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() } ?: "טרם נבחרה תיקייה")
    }
    var accessMessage by remember { mutableStateOf<String?>(null) }
    var storageEntries by remember {
        mutableStateOf(
            LocalAccessRepository.readUri(context, "storage_tree_uri")
                ?.let { LocalAccessRepository.listChildren(context, it) } ?: emptyList()
        )
    }
    var audioTitle by remember { mutableStateOf<String?>(null) }
    var audioPlaying by remember { mutableStateOf(false) }
    var audioPreparing by remember { mutableStateOf(false) }
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            runCatching { audioPlayer?.release() }
            audioPlayer = null
        }
    }

    SideEffect {
        val activity = context as? Activity
        activity?.window?.let { window ->
            if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            if (LocalAccessRepository.persistReadAccess(context, uri)) {
                LocalAccessRepository.saveUri(context, "local_model_uri", uri)
                modelName = LocalAccessRepository.displayName(context, uri)
                accessMessage = "הקובץ נבחר ונשמרה הרשאת קריאה. מנוע ההסקה המקומי יחובר בשלב הבא."
            } else {
                accessMessage = "ספק הקבצים לא אפשר שמירת הרשאה קבועה. נסה לבחור קובץ ממנהל קבצים אחר."
            }
        }
    }

    val storagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            if (LocalAccessRepository.persistTreeAccess(context, uri)) {
                LocalAccessRepository.saveUri(context, "storage_tree_uri", uri)
                storageName = uri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
                    ?: "תיקייה נבחרה"
                storageEntries = LocalAccessRepository.listChildren(context, uri)
                accessMessage = "נשמרה הרשאה לתיקייה שבחרת. נמצאו ${storageEntries.size} פריטים."
            } else {
                accessMessage = "לא ניתן לשמור הרשאה לתיקייה שנבחרה."
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                LocalAccessRepository.persistReadAccess(context, uri)
                audioPlayer?.release()
                val player = MediaPlayer()
                audioPlayer = player
                audioPreparing = true
                audioPlaying = false
                audioTitle = LocalAccessRepository.displayName(context, uri)
                player.setDataSource(context, uri)
                player.setOnPreparedListener { prepared ->
                    audioPreparing = false
                    prepared.start()
                    audioPlaying = true
                }
                player.setOnCompletionListener {
                    audioPlaying = false
                }
                player.setOnErrorListener { _, _, _ ->
                    audioPreparing = false
                    audioPlaying = false
                    accessMessage = "לא ניתן לנגן את הקובץ הזה במכשיר."
                    true
                }
                player.prepareAsync()
            }.onFailure {
                audioPreparing = false
                audioPlaying = false
                accessMessage = "פתיחת קובץ השמע נכשלה: ${it.message ?: "שגיאה לא ידועה"}"
            }
        }
    }

    if (showLegacyTools) {
        SmartPocketApp(onBack = { showLegacyTools = false })
        return
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentColor,
            secondary = if (accentName == "violet") MegaCyan else MegaPurple,
            background = MegaBackground,
            surface = MegaSurface,
            onPrimary = MegaBackground,
            onBackground = MegaText,
            onSurface = MegaText
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides scaledDensity
        ) {
            Scaffold(
                containerColor = MegaBackground,
                bottomBar = {
                    NavigationBar(containerColor = MegaSurface) {
                        NavigationBarItem(
                            selected = activeTab == "home",
                            onClick = { selectedModuleId = null; activeTab = "home" },
                            icon = { Text("⌂", fontSize = 21.sp) },
                            label = { Text("בית") }
                        )
                        NavigationBarItem(
                            selected = activeTab == "modules",
                            onClick = { selectedModuleId = null; activeTab = "modules" },
                            icon = { Text("▦", fontSize = 20.sp) },
                            label = { Text("מודולים") }
                        )
                        NavigationBarItem(
                            selected = activeTab == "settings",
                            onClick = { selectedModuleId = null; activeTab = "settings" },
                            icon = { Text("⚙", fontSize = 20.sp) },
                            label = { Text("הגדרות") }
                        )
                        NavigationBarItem(
                            selected = activeTab == "about",
                            onClick = { selectedModuleId = null; activeTab = "about" },
                            icon = { Text("ⓘ", fontSize = 20.sp) },
                            label = { Text("אודות") }
                        )
                    }
                }
            ) { scaffoldPadding ->
                val selectedModule = MegaModuleCatalog.modules.firstOrNull { it.id == selectedModuleId }
                when {
                    selectedModule != null -> ModuleDetailsScreen(
                        module = selectedModule,
                        modelName = modelName,
                        storageName = storageName,
                        storageEntries = storageEntries,
                        sortStorageBySize = sortStorageBySize,
                        audioTitle = audioTitle,
                        audioPlaying = audioPlaying,
                        audioPreparing = audioPreparing,
                        message = accessMessage,
                        onBack = { selectedModuleId = null; accessMessage = null },
                        onPickModel = { modelPicker.launch(arrayOf("*/*")) },
                        onPickStorage = { storagePicker.launch(null) },
                        onRefreshStorage = {
                            val selectedTree = LocalAccessRepository.readUri(context, "storage_tree_uri")
                            storageEntries = selectedTree?.let { LocalAccessRepository.listChildren(context, it) } ?: emptyList()
                            accessMessage = "הסריקה הסתיימה. נמצאו ${storageEntries.size} פריטים."
                        },
                        onOpenFile = { entry ->
                            if (!entry.isDirectory) {
                                try {
                                    val viewIntent = Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(entry.uri, entry.mimeType ?: "*/*")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(Intent.createChooser(viewIntent, "פתיחת קובץ"))
                                } catch (_: Exception) {
                                    accessMessage = "אין אפליקציה מתאימה לפתיחת הקובץ הזה."
                                }
                            }
                        },
                        onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
                        onToggleAudio = {
                            runCatching {
                                val player = audioPlayer
                                if (player != null) {
                                    if (player.isPlaying) {
                                        player.pause()
                                        audioPlaying = false
                                    } else {
                                        player.start()
                                        audioPlaying = true
                                    }
                                }
                            }.onFailure { accessMessage = "לא ניתן לשנות את מצב הניגון." }
                        },
                        onStopAudio = {
                            runCatching { audioPlayer?.stop() }
                            runCatching { audioPlayer?.release() }
                            audioPlayer = null
                            audioPlaying = false
                            audioPreparing = false
                        },
                        onOpenLegacy = { showLegacyTools = true }
                    )
                    activeTab == "settings" -> SettingsScreen(
                        compactCards = compactCards,
                        hidePlanned = hidePlannedModules,
                        showQuickActions = showQuickActions,
                        keepScreenOn = keepScreenOn,
                        sortBySize = sortStorageBySize,
                        accentName = accentName,
                        textScale = textScale,
                        startScreen = settingsPrefs.getString("start_screen", "home") ?: "home",
                        onCompactCards = { compactCards = it; settingsPrefs.edit().putBoolean("compact_cards", it).apply() },
                        onHidePlanned = { hidePlannedModules = it; settingsPrefs.edit().putBoolean("hide_planned", it).apply() },
                        onQuickActions = { showQuickActions = it; settingsPrefs.edit().putBoolean("quick_actions", it).apply() },
                        onKeepScreenOn = { keepScreenOn = it; settingsPrefs.edit().putBoolean("keep_screen_on", it).apply() },
                        onSortBySize = { sortStorageBySize = it; settingsPrefs.edit().putBoolean("sort_storage_size", it).apply() },
                        onAccent = { accentName = it; settingsPrefs.edit().putString("accent", it).apply() },
                        onTextScale = { textScale = it; settingsPrefs.edit().putFloat("text_scale", it).apply() },
                        onStartScreen = { settingsPrefs.edit().putString("start_screen", it).apply() },
                        onReset = {
                            compactCards = false
                            hidePlannedModules = false
                            showQuickActions = true
                            keepScreenOn = false
                            sortStorageBySize = false
                            accentName = "cyan"
                            textScale = 1f
                            settingsPrefs.edit().clear().apply()
                        }
                    )
                    activeTab == "about" -> AboutScreen()
                    activeTab == "modules" -> ModulesScreen(
                        modules = MegaModuleCatalog.modules,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        onOpen = { selectedModuleId = it },
                        compactCards = compactCards,
                        hidePlanned = hidePlannedModules
                    )
                    else -> HomeDashboard(
                        modules = MegaModuleCatalog.modules,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        onOpen = { selectedModuleId = it },
                        onOpenLegacy = { showLegacyTools = true },
                        padding = scaffoldPadding,
                        compactCards = compactCards,
                        hidePlanned = hidePlannedModules,
                        showQuickActions = showQuickActions
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    modules: List<MegaModule>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onOpenLegacy: () -> Unit,
    padding: PaddingValues,
    compactCards: Boolean,
    hidePlanned: Boolean,
    showQuickActions: Boolean
) {
    val filtered = modules.filter {
        (!hidePlanned || it.stage != ModuleStage.PLANNED) &&
            (searchText.isBlank() ||
            it.title.contains(searchText, ignoreCase = true) ||
            it.subtitle.contains(searchText, ignoreCase = true) ||
            it.description.contains(searchText, ignoreCase = true))
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(52.dp), shape = RoundedCornerShape(17.dp), color = MegaSurfaceAlt) {
                Box(contentAlignment = Alignment.Center) {
                    Text("MX", color = MegaCyan, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MEGA POCKET X", color = MegaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("כיס חכם • סביבת כלים אופליין", color = MegaMuted, fontSize = 12.sp)
            }
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF123B3C)) {
                Text("● אופליין", color = Color(0xFF75E6C2), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MegaSurfaceAlt)) {
            Column(
                modifier = Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFF183B5B), Color(0xFF29264F), Color(0xFF14283A))
                    )
                ).padding(21.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("כל הכוח. במקום אחד.", color = MegaText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("סביבת עבודה אישית עם כלים מקומיים, אחסון, מדיה, יצירה ו-AI במכשיר.",
                    color = Color(0xFFD2DDF0), fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("96", "כלים קיימים")
                    StatPill("12", "מחלקות")
                    StatPill("200+", "יעד תכונות")
                }
            }
        }

        if (showQuickActions) {
            Text("גישה מהירה", color = MegaText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickAction("🧰", "כלים פעילים", Modifier.weight(1f), onOpenLegacy)
                QuickAction("🧠", "AI מקומי", Modifier.weight(1f), { onOpen("ai") })
                QuickAction("📁", "אחסון", Modifier.weight(1f), { onOpen("storage") })
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            label = { Text("חיפוש מחלקה או כלי") },
            placeholder = { Text("לדוגמה: קבצים, AI, שמע...") }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("המחלקות שלך", color = MegaText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("12 מודולים", color = MegaMuted, fontSize = 12.sp)
        }
        ModuleGrid(modules = filtered, onOpen = onOpen, compactCards = compactCards)

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A35))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("פרטיות לפני הכול", color = MegaCyan, fontWeight = FontWeight.Bold)
                Text("האפליקציה אינה מבקשת הרשאת INTERNET. קבצים ותיקיות נבחרים דרך בורר Android ונשארים במכשיר.",
                    color = Color(0xFFD5E4EA), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
            Text(value, color = MegaText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xFFC4D2E6), fontSize = 10.sp)
        }
    }
}

@Composable
private fun QuickAction(icon: String, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(88.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = MegaSurface)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(5.dp))
            Text(title, color = MegaText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModuleGrid(modules: List<MegaModule>, onOpen: (String) -> Unit, compactCards: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modules.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { module ->
                    ModuleCard(module, Modifier.weight(1f), compactCards = compactCards) { onOpen(module.id) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (modules.isEmpty()) Text("לא נמצאו מחלקות מתאימות.", color = MegaMuted, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun ModuleCard(module: MegaModule, modifier: Modifier = Modifier, compactCards: Boolean = false, onClick: () -> Unit) {
    val statusColor = when (module.stage) {
        ModuleStage.ACTIVE -> Color(0xFF71E2B7)
        ModuleStage.FOUNDATION -> MegaCyan
        ModuleStage.PLANNED -> Color(0xFFB4A8FF)
    }
    Card(modifier = modifier.height(if (compactCards) 116.dp else 145.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = MegaSurface)) {
        Column(Modifier.fillMaxSize().padding(if (compactCards) 10.dp else 13.dp), verticalArrangement = Arrangement.spacedBy(if (compactCards) 4.dp else 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(module.icon, fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                Text(module.stage.label, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(module.title, color = MegaText, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(module.subtitle, color = MegaMuted, fontSize = 11.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ModulesScreen(
    modules: List<MegaModule>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    compactCards: Boolean,
    hidePlanned: Boolean
) {
    val filtered = modules.filter {
        (!hidePlanned || it.stage != ModuleStage.PLANNED) &&
            (searchText.isBlank() || it.title.contains(searchText, true) || it.description.contains(searchText, true))
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("כל המחלקות", color = MegaText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("המחלקות הפעילות מסומנות. שאר המודולים יתווספו בשלבים.", color = MegaMuted, fontSize = 13.sp)
        OutlinedTextField(value = searchText, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("חיפוש מחלקה") })
        ModuleGrid(filtered, onOpen, compactCards)
    }
}

@Composable
private fun ModuleDetailsScreen(
    module: MegaModule,
    modelName: String?,
    storageName: String,
    storageEntries: List<LocalFileEntry>,
    sortStorageBySize: Boolean,
    audioTitle: String?,
    audioPlaying: Boolean,
    audioPreparing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onPickModel: () -> Unit,
    onPickStorage: () -> Unit,
    onRefreshStorage: () -> Unit,
    onOpenFile: (LocalFileEntry) -> Unit,
    onPickAudio: () -> Unit,
    onToggleAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onOpenLegacy: () -> Unit
) {
    val displayedStorageEntries = if (sortStorageBySize) {
        storageEntries.sortedWith(compareBy<LocalFileEntry> { !it.isDirectory }.thenByDescending { it.sizeBytes })
    } else storageEntries

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("‹  חזרה", color = MegaCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 5.dp))
        Text(module.icon, fontSize = 42.sp)
        Text(module.title, color = MegaText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(module.subtitle, color = MegaCyan, fontSize = 14.sp)
        Text(module.description, color = MegaMuted, fontSize = 14.sp)
        Surface(color = MegaSurface, shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("מצב מודול: " + module.stage.label, color = MegaText, fontWeight = FontWeight.Bold)
                Text(
                    when (module.stage) {
                        ModuleStage.ACTIVE -> "הכלים במחלקה הזו זמינים להפעלה."
                        ModuleStage.FOUNDATION -> "התשתית הראשונית קיימת. יכולות העיבוד המלאות יתווספו בשלבים הבאים."
                        ModuleStage.PLANNED -> "המחלקה מופיעה במפת הפיתוח, אך עדיין לא מוצגת כיכולת פעילה."
                    },
                    color = MegaMuted, fontSize = 12.sp
                )
            }
        }
        when (module.id) {
            "legacy" -> Button(onClick = onOpenLegacy, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MegaCyan, contentColor = MegaBackground)) {
                Text("פתיחת 96 הכלים הפעילים")
            }
            "ai" -> {
                Text("מודל מקומי שנבחר", color = MegaText, fontWeight = FontWeight.Bold)
                Text(modelName ?: "לא נבחר קובץ מודל", color = MegaMuted, fontSize = 13.sp)
                Button(onClick = onPickModel, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MegaCyan, contentColor = MegaBackground)) {
                    Text("בחירת קובץ מודל מהמכשיר")
                }
                Text("הבחירה שומרת הרשאת קריאה לקובץ שנמצא במכשיר, בכרטיס SD או בהתקן USB. בשלב זה עדיין לא מחובר מנוע הרצה, ולכן בחירת מודל אינה מפעילה צ'אט AI.",
                    color = MegaMuted, fontSize = 12.sp)
            }
            "storage" -> {
                Text("תיקייה שנבחרה", color = MegaText, fontWeight = FontWeight.Bold)
                Text(storageName, color = MegaMuted, fontSize = 13.sp)
                Button(onClick = onPickStorage, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MegaCyan, contentColor = MegaBackground)) {
                    Text("בחירת תיקייה ב-SD או USB")
                }
                Button(onClick = onRefreshStorage, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MegaSurfaceAlt, contentColor = MegaText)) {
                    Text("סריקת התיקייה ורענון")
                }
                Text("נמצאו ${storageEntries.size} פריטים", color = MegaCyan, fontWeight = FontWeight.Bold)
                if (storageEntries.isEmpty()) {
                    Text("אין פריטים להצגה או שעדיין לא נבחרה תיקייה.", color = MegaMuted, fontSize = 12.sp)
                } else {
                    displayedStorageEntries.take(60).forEach { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().then(
                                if (entry.isDirectory) Modifier else Modifier.clickable { onOpenFile(entry) }
                            ),
                            color = MegaSurface,
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(if (entry.isDirectory) "📁" else "📄", fontSize = 20.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, color = MegaText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (entry.isDirectory) "תיקייה" else entry.mimeType ?: "קובץ",
                                        color = MegaMuted, fontSize = 10.sp)
                                }
                                if (!entry.isDirectory) Text("פתיחה", color = MegaCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Text("הגישה מוגבלת לתיקייה שבוחרים דרך Android. אין הרשאת אחסון גורפת; פתיחת קובץ מועברת לאפליקציה מקומית תואמת.",
                    color = MegaMuted, fontSize = 12.sp)
            }
            "media" -> {
                Text("נגן שמע מקומי", color = MegaText, fontWeight = FontWeight.Bold)
                Text(audioTitle ?: "לא נבחר קובץ שמע", color = MegaMuted, fontSize = 13.sp)
                Button(onClick = onPickAudio, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MegaCyan, contentColor = MegaBackground)) {
                    Text("בחירת קובץ שמע מהמכשיר / SD / USB")
                }
                if (audioPreparing) {
                    Text("מכין את הנגן…", color = MegaCyan, fontSize = 12.sp)
                } else if (audioTitle != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onToggleAudio, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MegaSurfaceAlt, contentColor = MegaText)) {
                            Text(if (audioPlaying) "השהיה" else "המשך ניגון")
                        }
                        Button(onClick = onStopAudio, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF592F40), contentColor = MegaText)) {
                            Text("עצירה")
                        }
                    }
                    Text("הניגון מתבצע במכשיר בלבד, ללא סטרימינג וללא אינטרנט.", color = MegaMuted, fontSize = 12.sp)
                }
            }
            else -> Text("המודול יפותח בהמשך לפי מפת הדרכים של MEGA POCKET X.", color = MegaMuted, fontSize = 13.sp)
        }
        if (!message.isNullOrBlank()) {
            Surface(color = Color(0xFF123B3C), shape = RoundedCornerShape(14.dp)) {
                Text(message, color = Color(0xFFB8F3E1), fontSize = 12.sp, modifier = Modifier.padding(13.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    compactCards: Boolean,
    hidePlanned: Boolean,
    showQuickActions: Boolean,
    keepScreenOn: Boolean,
    sortBySize: Boolean,
    accentName: String,
    textScale: Float,
    startScreen: String,
    onCompactCards: (Boolean) -> Unit,
    onHidePlanned: (Boolean) -> Unit,
    onQuickActions: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onSortBySize: (Boolean) -> Unit,
    onAccent: (String) -> Unit,
    onTextScale: (Float) -> Unit,
    onStartScreen: (String) -> Unit,
    onReset: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("מרכז ההגדרות", color = MegaText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("התאם את MEGA POCKET X לסגנון העבודה שלך", color = MegaMuted, fontSize = 13.sp)

        SettingsPanel("מראה ועיצוב", "🎨") {
            Text("צבע הדגשה", color = MegaText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("cyan" to "טורקיז", "violet" to "סגול", "emerald" to "ירוק", "amber" to "זהב")
                    .forEach { (id, label) ->
                        FilterChip(selected = accentName == id, onClick = { onAccent(id) }, label = { Text(label) })
                    }
            }
            Text("גודל טקסט: ${(textScale * 100).toInt()}%", color = MegaText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Slider(value = textScale, onValueChange = onTextScale, valueRange = 0.85f..1.25f, steps = 3)
            PreferenceRow("כרטיסים קומפקטיים", "הצגת יותר מודולים בכל מסך", compactCards, onCompactCards)
            PreferenceRow("הסתרת מודולים שבתכנון", "הצג רק כלים פעילים ותשתיות מוכנות", hidePlanned, onHidePlanned)
            PreferenceRow("קיצורי דרך במסך הבית", "גישה מהירה לכלים, AI ואחסון", showQuickActions, onQuickActions)
        }

        SettingsPanel("קבצים ואחסון", "📁") {
            PreferenceRow("מיון לפי גודל", "הצג תיקיות תחילה ולאחר מכן קבצים מהגדול לקטן", sortBySize, onSortBySize)
        }

        SettingsPanel("התנהגות", "⚙️") {
            PreferenceRow("השארת המסך דולק", "מונע כיבוי מסך כל עוד האפליקציה פתוחה", keepScreenOn, onKeepScreenOn)
        }

        SettingsPanel("מסך פתיחה", "🚀") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("home" to "בית", "modules" to "מודולים", "settings" to "הגדרות", "about" to "אודות")
                    .forEach { (id, label) ->
                        FilterChip(selected = startScreen == id, onClick = { onStartScreen(id) }, label = { Text(label) })
                    }
            }
            Text("הבחירה תופעל בפעם הבאה שהאפליקציה תיפתח.", color = MegaMuted, fontSize = 11.sp)
        }

        SettingsPanel("פרטיות ומערכת", "🛡️") {
            SettingsInfo("תקשורת", "אין הרשאת INTERNET באפליקציה")
            SettingsInfo("שמירה", "העדפות והרשאות נשמרות במכשיר")
            SettingsInfo("קבצים", "גישה רק לפריטים שנבחרו דרך Android")
            SettingsInfo("תאימות", "Android 8 ומעלה • יעד Android 13")
        }

        Button(onClick = onReset, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B2935), contentColor = Color(0xFFFFDCE3))) {
            Text("איפוס הגדרות")
        }
        Text("גרסה 2.2 • כל ההגדרות נשמרות מקומית.", color = MegaMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsPanel(title: String, icon: String, content: @Composable () -> Unit) {
    Surface(color = MegaSurface, shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, fontSize = 18.sp)
                Text(title, color = MegaCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            content()
        }
    }
}

@Composable
private fun PreferenceRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = MegaText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = MegaMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsInfo(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = MegaText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(value, color = MegaMuted, fontSize = 11.sp)
    }
}

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(30.dp), color = MegaSurfaceAlt, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("MX", color = MegaCyan, fontSize = 40.sp, fontWeight = FontWeight.Black)
            }
        }
        Text("MEGA POCKET X", color = MegaText, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("כיס חכם • כלים עוצמתיים במקום אחד", color = MegaCyan, fontSize = 14.sp)
        SettingsPanel("אודות האפליקציה", "✨") {
            Text("סביבת כלים מקומית לאנדרואיד בעברית ובכיוון כתיבה מימין לשמאל. הפרויקט מרכז כלי עבודה, קבצים, מדיה, יצירה וכלים חכמים באפליקציה אחת, תוך שמירה על פרטיות ועבודה ללא חיבור רשת.",
                color = MegaMuted, fontSize = 13.sp)
            Text("פותח על ידי ספרדי גאה", color = MegaCyan, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        SettingsPanel("פרטי גרסה", "📋") {
            SettingsInfo("גרסה", "2.2 – MEGA POCKET X")
            SettingsInfo("מערכת יעד", "Android 13 • API 33")
            SettingsInfo("טכנולוגיה", "Kotlin • Jetpack Compose")
            SettingsInfo("מצב עבודה", "אופליין • ללא חשבון • ללא שרת")
            SettingsInfo("כלים קיימים", "96 כלים מהגרסה הקודמת; מחלקות נוספות בתהליך פיתוח")
        }
        SettingsPanel("פרטיות", "🔒") {
            SettingsInfo("תקשורת", "אין הרשאת INTERNET")
            SettingsInfo("אחסון", "גישה לפי בחירת המשתמש בלבד")
            SettingsInfo("שקיפות", "יכולות בתכנון מסומנות בנפרד מכלים פעילים")
        }
        Text("© 2026 MEGA POCKET X", color = MegaMuted, fontSize = 11.sp)
    }
}

