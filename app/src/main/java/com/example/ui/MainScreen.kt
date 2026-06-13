package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ScheduledMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Custom colors based on Geometric Balance
val GeoPurpleMain = Color(0xFF6750A4)
val GeoPurpleBg = Color(0xFFFDF8FD)
val GeoPillActive = Color(0xFFE8DEF8)
val GeoServiceBg = Color(0xFFEADDFF)
val GeoServiceText = Color(0xFF21005D)
val GeoBottomNavBg = Color(0xFFF3EDF7)
val GeoTextDark = Color(0xFF0F172A)
val GeoBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    
    // Bottom sheet dialog controls for adding and editing
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedMessageToEdit by remember { mutableStateOf<ScheduledMessage?>(null) }

    // Navigation simulated state: "home" (Ana Ekran), "drafts" (Taslaklar), "history" (Geçmiş)
    var selectedTab by remember { mutableStateOf("home") }

    // Launcher for READ_CONTACTS permission request
    var pendingContactPickerRequest by remember { mutableStateOf(false) }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingContactPickerRequest = true
        } else {
            Toast.makeText(context, "Rehberden seçim yapabilmek için rehber izni zorunludur.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher to Pick Contact
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { contactUri ->
            val result = retrieveContactDetails(context, contactUri)
            if (result != null) {
                // Open add/edit dialog with selected contact prefilled
                selectedMessageToEdit = ScheduledMessage(
                    contactName = result.first,
                    phoneNumber = result.second,
                    messageText = "",
                    scheduledTime = System.currentTimeMillis() + 600000 // 10 minutes from now
                )
                showAddEditDialog = true
            } else {
                Toast.makeText(context, "Kişi bilgileri rehberden okunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto trigger contact picker once permission is confirmed
    if (pendingContactPickerRequest) {
        LaunchedEffect(Unit) {
            pendingContactPickerRequest = false
            try {
                contactPickerLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Rehber açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.isNotificationPermissionGranted = isGranted
    }

    // Refresh states and permissions on launch and resume
    LaunchedEffect(Unit) {
        viewModel.refreshSchedulerState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.isNotificationPermissionGranted) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = GeoPurpleBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.5).sp,
                    color = GeoTextDark
                )
                
                // Rounded profile circle indicating the user
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF3EDF7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil",
                        tint = GeoPurpleMain,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Elegant Simulated Bottom Navigation Bar containing 3 tabs: Ana Ekran, Taslaklar, Geçmiş
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(GeoBottomNavBg)
                    .border(
                        BorderStroke(1.dp, GeoBorderColor),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ana Ekran (Home / Pending) Tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedTab = "home" }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selectedTab == "home") GeoPillActive else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏠", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ANA SAYFA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "home") GeoTextDark else Color.Gray
                        )
                    }

                    // Taslaklar (Drafts) Tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedTab = "drafts" }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selectedTab == "drafts") GeoPillActive else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📃", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TASLAKLAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "drafts") GeoTextDark else Color.Gray
                        )
                    }

                    // Geçmiş (History) Tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedTab = "history" }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selectedTab == "history") GeoPillActive else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📊", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GEÇMİŞ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "history") GeoTextDark else Color.Gray
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Floating Action Button prompts a direct contact picker dialog
            FloatingActionButton(
                onClick = {
                    // Check contacts permission then load
                    val permissionCheck = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                    if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        try {
                            contactPickerLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Rehber yüklenirken hata oluştu.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                },
                containerColor = GeoPurpleMain,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .offset(y = 20.dp)
                    .height(56.dp)
                    .width(56.dp)
                    .testTag("add_schedule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Rehberden Kişi Seç", modifier = Modifier.size(24.dp))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GeoPurpleBg)
        ) {
            // Main Service Switch Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("master_switch_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GeoServiceBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SERVİS DURUMU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GeoServiceText.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (viewModel.isSchedulerActive) "Arka Planda Aktif" else "Hizmet Pasif",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GeoServiceText
                        )
                    }
                    
                    Switch(
                        checked = viewModel.isSchedulerActive,
                        onCheckedChange = { active ->
                            viewModel.toggleScheduler(active)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GeoPurpleMain,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("master_on_off_switch")
                    )
                }
            }

            Text(
                text = "Casper VIA X40 veya benzeri telefonlarda kilit ekranı dahil kesintisiz gönderim sağlar.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alert block if permissions are missing
            AnimatedVisibility(
                visible = !viewModel.isNotificationPermissionGranted ||
                        !viewModel.isExactAlarmPermissionGranted ||
                        !viewModel.isAccessibilityPermissionGranted,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEA580C))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sistem İzin İstekleri",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PermissionRow(
                                title = "Bildirimler",
                                isGranted = viewModel.isNotificationPermissionGranted,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                            PermissionRow(
                                title = "Hassas Alarm / Takvim",
                                isGranted = viewModel.isExactAlarmPermissionGranted,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            PermissionRow(
                                title = "Erişilebilirlik (Gönder butonu tıklaması için)",
                                isGranted = viewModel.isAccessibilityPermissionGranted,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // Headings based on selected tab state
            val titleText = when (selectedTab) {
                "drafts" -> "Taslak Mesajlar"
                "history" -> "Mesaj Geçmişi"
                else -> "Zamanlanan Mesajlar"
            }

            Text(
                text = titleText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Database Message Filter mapping
            val displayMessages = when (selectedTab) {
                "drafts" -> messages.filter { it.status == "DRAFT" }
                "history" -> messages.filter { it.status == "SENT" || it.status == "FAILED" }
                else -> messages.filter { it.status == "PENDING" }
            }

            if (displayMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (selectedTab) {
                                "drafts" -> "📃"
                                "history" -> "📊"
                                else -> "💬"
                            },
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedTab) {
                                "drafts" -> "Taslak bulunmuyor"
                                "history" -> "Kayıtlı geçmiş gönderim bulunmuyor"
                                else -> "Bekleyen zamanlı mesaj bulunmuyor"
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sağ alttaki (+) simgesine basıp rehberinizden kişi seçerek işlem yapabilirsiniz.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayMessages, key = { it.id }) { msg ->
                        SwipeableMessageItem(
                            message = msg,
                            onDelete = {
                                viewModel.deleteMessage(msg)
                                Toast.makeText(context, "Hizmet iptal edildi ve silindi.", Toast.LENGTH_SHORT).show()
                            },
                            onDraft = {
                                val draftedMsg = msg.copy(status = "DRAFT")
                                viewModel.updateScheduledMessage(draftedMsg)
                                Toast.makeText(context, "Mesaj taslaklar arasına taşındı.", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                selectedMessageToEdit = msg
                                showAddEditDialog = true
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }

        // Add/Edit Message Editor Sheet modal
        if (showAddEditDialog) {
            AddScheduleBottomSheet(
                editableMessage = selectedMessageToEdit,
                onDismiss = {
                    showAddEditDialog = false
                    selectedMessageToEdit = null
                },
                onSave = { updatedMsg ->
                    if (updatedMsg.id == 0) {
                        viewModel.addScheduledMessageWithStatus(
                            contactName = updatedMsg.contactName,
                            phoneNumber = updatedMsg.phoneNumber,
                            messageText = updatedMsg.messageText,
                            scheduledTime = updatedMsg.scheduledTime,
                            status = updatedMsg.status
                        )
                        Toast.makeText(context, "Mesaj başarıyla planlandı.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateScheduledMessage(updatedMsg)
                        Toast.makeText(context, "Mesaj güncellendi.", Toast.LENGTH_SHORT).show()
                    }
                    showAddEditDialog = false
                    selectedMessageToEdit = null
                    selectedTab = if (updatedMsg.status == "DRAFT") "drafts" else "home"
                }
            )
        }
    }
}

@Composable
fun SwipeableMessageItem(
    message: ScheduledMessage,
    onDelete: () -> Unit,
    onDraft: () -> Unit,
    onEdit: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
    ) {
        // Dynamic drag/slide feedback background colors
        if (offsetX > 0f) {
            // Left to Right: red delete panel
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFFFF1F1))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SİLMEK İÇİN BIRAKIN",
                        color = Color(0xFF991B1B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (offsetX < 0f && message.status != "DRAFT") {
            // Right to Left: purple draft panel
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFFAF5FF))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TASLAĞA GÖNDERİN",
                        color = Color(0xFF6B21A8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.6.dp))
                    Icon(imageVector = Icons.Default.Class, contentDescription = "Taslak", tint = Color(0xFF9333EA))
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 260f) {
                                onDelete()
                            } else if (offsetX < -260f && message.status != "DRAFT") {
                                onDraft()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Damp swipe
                            val checkVal = offsetX + dragAmount
                            if (checkVal in -400f..400f) {
                                offsetX = checkVal
                            }
                        }
                    )
                }
        ) {
            MessageItem(
                message = message,
                onDelete = onDelete,
                onEdit = onEdit
            )
        }
    }
}

@Composable
fun MessageItem(
    message: ScheduledMessage,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale("tr")) }
    val timeString = formatter.format(Date(message.scheduledTime))
    val dayLabel = remember(message.scheduledTime) { getDayLabel(message.scheduledTime) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_item_${message.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GeoBorderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon block 💬 or check representing task
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (message.status == "PENDING") Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (message.status) {
                        "SENT" -> "✔"
                        "DRAFT" -> "📃"
                        else -> "💬"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (message.status == "PENDING") Color(0xFF15803D) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.contactName.ifEmpty { "Kişi İsmi Yok" },
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = GeoTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val dayColor = when (dayLabel) {
                        "BUGÜN" -> Color(0xFF64748B)
                        "YARIN" -> GeoPurpleMain
                        else -> Color(0xFF128C7E)
                    }

                    Text(
                        text = if (message.status == "DRAFT") "TASLAK" else if (message.status == "FAILED") "İPTAL" else dayLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (message.status == "DRAFT") Color(0xFF7C3AED) else if (message.status == "FAILED") Color(0xFFEF4444) else dayColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = message.messageText,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⏰ $timeString",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(text = "•", fontSize = 11.sp, color = Color.LightGray)
                    Text(
                        text = "WhatsApp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(text = "•", fontSize = 11.sp, color = Color.LightGray)
                    Text(
                        text = message.phoneNumber,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action row for direct editing or removal
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit Button (Mesaj Düzenle)
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Zamanlamayı Düzenle",
                        tint = GeoPurpleMain,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Zamanlamayı Sil",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isGranted) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF16A34A) else Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isGranted) Color(0xFF14532D) else Color(0xFF7F1D1D)
            )
        }
        if (!isGranted) {
            Text(
                text = "İzin Ver",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleBottomSheet(
    editableMessage: ScheduledMessage?,
    onDismiss: () -> Unit,
    onSave: (ScheduledMessage) -> Unit
) {
    val context = LocalContext.current
    var contactName by remember { mutableStateOf(editableMessage?.contactName ?: "") }
    var phoneNumber by remember { mutableStateOf(editableMessage?.phoneNumber ?: "") }
    var messageText by remember { mutableStateOf(editableMessage?.messageText ?: "") }

    val calendar = remember { Calendar.getInstance() }
    val initialTime = editableMessage?.scheduledTime ?: (System.currentTimeMillis() + 600000)
    var selectedCalendar by remember { mutableStateOf(calendar.clone() as Calendar).apply { value.timeInMillis = initialTime } }

    var dateString by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("tr")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("tr")) }

    // Init Date strings
    LaunchedEffect(initialTime) {
        selectedCalendar.timeInMillis = initialTime
        dateString = dateFormatter.format(selectedCalendar.time)
        timeString = timeFormatter.format(selectedCalendar.time)
    }

    // Dropdown/Simulated selection option for status
    var selectedStatus by remember { mutableStateOf(editableMessage?.status ?: "PENDING") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Elegant Editor Top Header carrying the requested Right Top Onay Button!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editableMessage == null || editableMessage.id == 0) "Yeni Mesaj Zamanla" else "Mesajı Düzenle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = GeoTextDark
                )

                // High priority Checkmark Onay Button to save changes and return immediately
                IconButton(
                    onClick = {
                        if (contactName.isNotBlank() && phoneNumber.isNotBlank() && messageText.isNotBlank() && dateString.isNotBlank() && timeString.isNotBlank()) {
                            val savedMsg = ScheduledMessage(
                                id = editableMessage?.id ?: 0,
                                contactName = contactName,
                                phoneNumber = phoneNumber,
                                messageText = messageText,
                                scheduledTime = selectedCalendar.timeInMillis,
                                status = selectedStatus
                            )
                            onSave(savedMsg)
                        } else {
                            Toast.makeText(context, "Lütfen tüm bilgileri eksiksiz doldurun.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(GeoPillActive, CircleShape)
                        .testTag("onay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Onayla ve Kaydet",
                        tint = GeoPurpleMain,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Contact Name Input
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Kişi İsmi") },
                placeholder = { Text("Örneğin: Fatma Teyze") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GeoPurpleMain) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_name_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Telefon Numarası") },
                placeholder = { Text("Örneğin: +905XXXXXXXXX") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GeoPurpleMain) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = { Text("Lütfen ülke kodu (+90) eklemeyi unutmayın.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_number_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Message Content Body Input
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("WhatsApp Mesaj İçeriği") },
                placeholder = { Text("Gönderilecek mesaj metni...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_text_input")
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date picker trigger button
                OutlinedButton(
                    onClick = {
                        val dp = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedCalendar.set(Calendar.YEAR, year)
                                selectedCalendar.set(Calendar.MONTH, month)
                                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                dateString = dateFormatter.format(selectedCalendar.time)
                            },
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH),
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dp.show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("date_picker_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPurpleMain)
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = dateString.ifEmpty { "Tarih Seç" }, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }

                // Time picker trigger button
                OutlinedButton(
                    onClick = {
                        val tp = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                selectedCalendar.set(Calendar.MINUTE, minute)
                                selectedCalendar.set(Calendar.SECOND, 0)
                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                timeString = timeFormatter.format(selectedCalendar.time)
                            },
                            selectedCalendar.get(Calendar.HOUR_OF_DAY),
                            selectedCalendar.get(Calendar.MINUTE),
                            true
                        )
                        tp.show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("time_picker_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPurpleMain)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeString.ifEmpty { "Saat Seç" }, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Mode options (Planla vs Taslak)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Kayıt Tipi:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedStatus == "PENDING") GeoPillActive else Color(0xFFF1F5F9))
                        .clickable { selectedStatus = "PENDING" }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Planlı Gönder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedStatus == "PENDING") GeoPurpleMain else Color.Gray)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedStatus == "DRAFT") GeoPillActive else Color(0xFFF1F5F9))
                        .clickable { selectedStatus = "DRAFT" }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Taslak Olarak Tut", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedStatus == "DRAFT") GeoPurpleMain else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(text = "İptal", color = Color.Gray)
                }

                Button(
                    onClick = {
                        if (contactName.isNotBlank() && phoneNumber.isNotBlank() && messageText.isNotBlank() && dateString.isNotBlank() && timeString.isNotBlank()) {
                            val msg = ScheduledMessage(
                                id = editableMessage?.id ?: 0,
                                contactName = contactName,
                                phoneNumber = phoneNumber,
                                messageText = messageText,
                                scheduledTime = selectedCalendar.timeInMillis,
                                status = selectedStatus
                            )
                            onSave(msg)
                        } else {
                            Toast.makeText(context, "Lütfen alanları eksiksiz girin.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("schedule_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPurpleMain)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Onayla ve Kaydet")
                }
            }
        }
    }
}

// Internal helper logic for day tags
private fun getDayLabel(scheduledTime: Long): String {
    val currentCal = Calendar.getInstance()
    val scheduledCal = Calendar.getInstance().apply { timeInMillis = scheduledTime }
    
    return if (currentCal.get(Calendar.YEAR) == scheduledCal.get(Calendar.YEAR) &&
        currentCal.get(Calendar.DAY_OF_YEAR) == scheduledCal.get(Calendar.DAY_OF_YEAR)) {
        "BUGÜN"
    } else {
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (tomorrowCal.get(Calendar.YEAR) == scheduledCal.get(Calendar.YEAR) &&
            tomorrowCal.get(Calendar.DAY_OF_YEAR) == scheduledCal.get(Calendar.DAY_OF_YEAR)) {
            "YARIN"
        } else {
            "GELECEK"
        }
    }
}

// Contacts Database Query implementation
private fun retrieveContactDetails(context: Context, contactUri: Uri): Pair<String, String>? {
    var rawName = ""
    var rawPhone = ""
    val cr = context.contentResolver
    
    cr.query(contactUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idCol = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameCol = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            
            val contactId = if (idCol >= 0) cursor.getString(idCol) else ""
            rawName = if (nameCol >= 0) cursor.getString(nameCol) else ""
            
            val hasPhoneCol = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            val hasPhone = if (hasPhoneCol >= 0) cursor.getString(hasPhoneCol) else "0"
            
            if (hasPhone == "1" && contactId.isNotEmpty()) {
                cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val numCol = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numCol >= 0) {
                            rawPhone = phoneCursor.getString(numCol)
                        }
                    }
                }
            }
        }
    }
    
    // Normalize contact number for seamless WhatsApp linking
    if (rawPhone.isNotEmpty()) {
        // Remove spaces, dashes, parentheses
        var clean = rawPhone.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            
        // If it starts with 0 and doesn't have country code, prefix with Turkish country code (+90) by default
        if (clean.startsWith("0")) {
            clean = "+90" + clean.substring(1)
        } else if (!clean.startsWith("+")) {
            clean = "+$clean"
        }
        rawPhone = clean
    }

    return if (rawName.isNotEmpty() || rawPhone.isNotEmpty()) rawName to rawPhone else null
}
