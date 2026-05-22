package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.api.model.DeficiencyItem
import com.example.data.model.DeficiencyAnalysis
import com.example.data.model.IntakeRecord
import com.example.ui.viewmodel.DeficiencyUiState
import com.example.ui.viewmodel.NutriViewModel
import com.example.ui.viewmodel.ScanUiState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// Dynamic nutrition facts label preview drawer helper
fun generateLabelBitmap(foodName: String, calories: Double, protein: Double, fiber: Double): Bitmap {
    val width = 450
    val height = 650
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    // White Board canvas
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Dark Bold Borders
    paint.color = android.graphics.Color.BLACK
    paint.strokeWidth = 10f
    paint.style = android.graphics.Paint.Style.STROKE
    canvas.drawRect(15f, 15f, (width - 15).toFloat(), (height - 15).toFloat(), paint)

    // Title text
    paint.style = android.graphics.Paint.Style.FILL
    paint.textSize = 42f
    paint.isFakeBoldText = true
    canvas.drawText("Nutrition Facts", 30f, 75f, paint)

    // Sub Title
    paint.textSize = 18f
    paint.isFakeBoldText = false
    canvas.drawText("Serving Size: 1 Container", 30f, 105f, paint)

    // Divider
    paint.strokeWidth = 8f
    canvas.drawLine(20f, 120f, (width - 20).toFloat(), 120f, paint)

    // Product Header
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText(foodName, 30f, 165f, paint)

    paint.strokeWidth = 4f
    canvas.drawLine(20f, 185f, (width - 20).toFloat(), 185f, paint)

    // Calories Header
    paint.textSize = 24f
    paint.isFakeBoldText = false
    canvas.drawText("Amount Per Serving", 30f, 220f, paint)

    paint.textSize = 38f
    paint.isFakeBoldText = true
    canvas.drawText("Calories", 30f, 270f, paint)
    canvas.drawText("${calories.toInt()}", (width - 120).toFloat(), 270f, paint)

    paint.strokeWidth = 6f
    canvas.drawLine(20f, 290f, (width - 20).toFloat(), 290f, paint)

    // Values columns
    paint.textSize = 22f
    paint.isFakeBoldText = false
    canvas.drawText("Total Fat", 30f, 335f, paint)
    paint.isFakeBoldText = true
    canvas.drawText("2.5g", (width - 110).toFloat(), 335f, paint)

    paint.strokeWidth = 2f
    canvas.drawLine(20f, 355f, (width - 20).toFloat(), 355f, paint)

    paint.isFakeBoldText = false
    canvas.drawText("Dietary Fiber", 30f, 400f, paint)
    paint.isFakeBoldText = true
    canvas.drawText("${fiber}g", (width - 110).toFloat(), 400f, paint)

    paint.strokeWidth = 2f
    canvas.drawLine(20f, 420f, (width - 20).toFloat(), 420f, paint)

    paint.isFakeBoldText = false
    canvas.drawText("Protein", 30f, 465f, paint)
    paint.isFakeBoldText = true
    canvas.drawText("${protein}g", (width - 110).toFloat(), 465f, paint)

    paint.strokeWidth = 6f
    canvas.drawLine(20f, 490f, (width - 20).toFloat(), 490f, paint)

    // Vitamin lines
    paint.textSize = 18f
    paint.isFakeBoldText = false
    canvas.drawText("Calcium 10%  •  Iron 25%  •  Vitamin B 15%", 30f, 530f, paint)
    canvas.drawText("Potassium 8%  •  Vitamin D 45%", 30f, 560f, paint)

    // Footer lines
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 14f
    canvas.drawText("Ingredients: Ground Oats, Protein Blend, Fiber Seed,", 30f, 600f, paint)
    canvas.drawText("Natural Coconut Flakes, Cane Extract, Minerals.", 30f, 625f, paint)

    return bitmap
}

sealed class AppTab(val title: String, val icon: ImageVector) {
    object Dashboard : AppTab("Dashboard", Icons.Default.Home)
    object Scanner : AppTab("Scanner", Icons.Default.CameraAlt)
    object Deficiencies : AppTab("Deficiencies", Icons.Default.HealthAndSafety)
}

@Composable
fun NutriTrackApp(viewModel: NutriViewModel) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        WelcomeLoginScreen(viewModel = viewModel)
    } else {
        var selectedTab by remember { mutableStateOf<AppTab>(AppTab.Dashboard) }

        // Edge to Edge light grey canvas of High Density theme
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF3F4F9),
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = Color(0xFFDDE3EA),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
                    listOf(AppTab.Dashboard, AppTab.Scanner, AppTab.Deficiencies).forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF001D36),
                                selectedTextColor = Color(0xFF001D36),
                                unselectedIconColor = Color(0xFF44474E),
                                unselectedTextColor = Color(0xFF44474E),
                                indicatorColor = Color(0xFFD1E4FF)
                            )
                        )
                    }
                }
            },
            containerColor = Color(0xFFF3F4F9)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.Dashboard -> DashboardScreen(viewModel = viewModel, onNavigateToTab = { selectedTab = it })
                        AppTab.Scanner -> ScannerScreen(viewModel = viewModel)
                        AppTab.Deficiencies -> DeficienciesScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ======================== DASHBOARD SCREEN ========================

@Composable
fun DashboardScreen(viewModel: NutriViewModel, onNavigateToTab: (AppTab) -> Unit) {
    val todaysIntakes by viewModel.todaysIntakes.collectAsState()
    val totals by viewModel.todaysTotals.collectAsState()
    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetFiber by viewModel.targetFiber.collectAsState()
    val allDeficiencyAnalyses by viewModel.allDeficiencyAnalyses.collectAsState()

    var showTargetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // App Header - High Density Theme Style
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF0061A4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "NutriTrack Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "NutriTrack",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Smart Nutrient & Deficiency Tracker",
                            fontSize = 11.sp,
                            color = Color(0xFF44474E)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFD1E4FF), CircleShape)
                        .background(Color.White)
                        .clickable { showTargetDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Edit Targets",
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            val userEmailOrPhone by viewModel.userEmailOrPhone.collectAsState()
            val userGeminiToken by viewModel.userGeminiToken.collectAsState()
            val context = LocalContext.current

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E4FF).copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFD1E4FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Profile",
                                tint = Color(0xFF004F80),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Session: $userEmailOrPhone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003355)
                            )
                        }
                        Text(
                            text = "Sign Out",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA1A1A),
                            modifier = Modifier
                                .clickable { viewModel.logout() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Masked Gemini token
                    val maskedToken = if (userGeminiToken.length > 8) {
                        userGeminiToken.take(4) + "..." + userGeminiToken.takeLast(4)
                    } else if (userGeminiToken.isNotBlank()) {
                        "••••••••"
                    } else {
                        "None (AI disabled)"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Gemini Token",
                                tint = Color(0xFF027A48),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Gemini key: $maskedToken",
                                fontSize = 11.sp,
                                color = Color(0xFF004F80).copy(alpha = 0.8f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clickable {
                                    if (userEmailOrPhone.isNotBlank() && userEmailOrPhone.contains("@")) {
                                        val refreshed = viewModel.resolveTokenFromEmail(userEmailOrPhone)
                                        viewModel.saveGeminiToken(refreshed)
                                        Toast.makeText(context, "Gemini credentials verified and synced", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Credentials Synced",
                                tint = Color(0xFF027A48),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Auto-Synced",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF027A48)
                            )
                        }
                    }
                }
            }
        }

        // Main Progress Summary (Calories card & progress percentage indicator)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY CALORIES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF44474E),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${totals.first.toInt()}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color(0xFF001D36)
                                )
                                Text(
                                    text = " / ${targetCalories.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF44474E),
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                        }

                        // Progress representation
                        val progress = if (targetCalories > 0) (totals.first / targetCalories).toFloat() else 0f
                        val percentage = (progress * 100).toInt().coerceAtMost(100)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color(0xFFE1E2EC),
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFF0061A4),
                                    startAngle = -90f,
                                    sweepAngle = (progress * 360f).coerceAtMost(360f),
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$percentage%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0061A4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inner grid cards for Protein and Fiber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Protein
                        val proteinProgress = if (targetProtein > 0) (totals.second / targetProtein).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD1E4FF))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "PROTEIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001D36)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${totals.second.toInt()}g",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001D36)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(proteinProgress)
                                            .background(Color(0xFF0061A4))
                                    )
                                }
                            }
                        }

                        // Fiber
                        val fiberProgress = if (targetFiber > 0) (totals.third / targetFiber).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE6DEFF))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "FIBER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1735)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${totals.third.toInt()}g",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1735)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fiberProgress)
                                            .background(Color(0xFF625B71))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Navigation shortcuts grid matching HTML design
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE3EA)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(108.dp)
                        .clickable { onNavigateToTab(AppTab.Scanner) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Label",
                            tint = Color(0xFF001D36),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan Food Label",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE3EA)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(108.dp)
                        .clickable { onNavigateToTab(AppTab.Deficiencies) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Vitamin Tools",
                            tint = Color(0xFF001D36),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Vitamin Tools",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Deficiency alert banner
        item {
            val alertsInfo = allDeficiencyAnalyses.firstOrNull()
            val hasAlerts = alertsInfo != null
            val subText = if (hasAlerts) {
                "Recent evaluation suggests possible risks. Tap to view."
            } else {
                "Potential low Vitamin D (2.1mcg). Tap to analyze."
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE0FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(AppTab.Deficiencies) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = "Deficiency warning",
                                tint = Color(0xFF77536D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Deficiency Alert",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2A1826)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subText,
                                fontSize = 11.sp,
                                color = Color(0xFF52434E)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go to analysis",
                        tint = Color(0xFF2A1826)
                    )
                }
            }
        }

        // Intake logging heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Intake (${todaysIntakes.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1E)
                )

                var showAddDialog by remember { mutableStateOf(false) }
                if (showAddDialog) {
                    ManualAddDialog(
                        onDismiss = { showAddDialog = false },
                        viewModel = viewModel,
                        onAdd = { name, cal, prot, fib, vits ->
                            viewModel.addIntake(name, cal, prot, fib, vits)
                            showAddDialog = false
                        }
                    )
                }

                TextButton(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0061A4))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Manual", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Food Intake List - styled inside background cards
        if (todaysIntakes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "No foods logged",
                            tint = Color(0xFF44474E),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No foods logged today",
                            color = Color(0xFF191C1E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan a nutrition table image or add manually using the buttons to get started tracking targets.",
                            color = Color(0xFF44474E),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todaysIntakes) { intake ->
                FoodIntakeRow(intake = intake, onDelete = { viewModel.deleteIntake(intake) })
            }
        }
    }

    if (showTargetDialog) {
        TargetInputDialog(
            currentCalories = targetCalories,
            currentProtein = targetProtein,
            currentFiber = targetFiber,
            onDismiss = { showTargetDialog = false },
            onSave = { cal, prot, fib ->
                viewModel.setTargetCalories(cal)
                viewModel.setTargetProtein(prot)
                viewModel.setTargetFiber(fib)
                showTargetDialog = false
            }
        )
    }
}

@Composable
fun NutrientRing(
    currentValue: Double,
    targetValue: Double,
    label: String,
    unit: String,
    color: Color
) {
    val progress = if (targetValue > 0) (currentValue / targetValue).toFloat() else 0f
    val sweptAngle = (progress * 360f).coerceAtMost(360f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(110.dp)
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            // Track background arc - High Density grey
            drawCircle(
                color = Color(0xFFE1E2EC),
                radius = size.minDimension / 2f,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Color sweep progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweptAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${currentValue.toInt()}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF001D36)
            )
            Text(
                text = "/ ${targetValue.toInt()} $unit",
                fontSize = 10.sp,
                color = Color(0xFF44474E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun NutrientProgressBar(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    color: Color
) {
    val fraction = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.width(160.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = label, color = Color(0xFF191C1E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${current.toInt()}/${target.toInt()}$unit",
                color = Color(0xFF44474E),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFE1E2EC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun FoodIntakeRow(intake: IntakeRecord, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("intake_item_${intake.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFD1E4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = "Food",
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = intake.foodName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${intake.caloriesKcal.toInt()} kcal",
                            color = Color(0xFF44474E),
                            fontSize = 11.sp
                        )
                        Text(text = "•", color = Color(0xFFDDE3EA))
                        Text(
                            text = "Protein: ${intake.proteinGrams}g",
                            color = Color(0xFF0061A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = "•", color = Color(0xFFDDE3EA))
                        Text(
                            text = "Fiber: ${intake.fiberGrams}g",
                            color = Color(0xFF625B71),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (intake.vitamins.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💊 Vitamins: ${intake.vitamins}",
                            color = Color(0xFF027A48),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ================= PLAYGROUND SCANNER SCREEN ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(viewModel: NutriViewModel) {
    val context = LocalContext.current
    val scanUiState by viewModel.scanUiState.collectAsState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmapToScan by remember { mutableStateOf<Bitmap?>(null) }

    // Test label presets generator list so players can test inside emulators instantly!
    val presets = listOf(
        Pair("Protein Shake Label", Triple(240.0, 30.0, 1.0)),
        Pair("High-Fiber Oatmeal Cup", Triple(190.0, 7.5, 9.0)),
        Pair("Organic Power Seed Bar", Triple(160.0, 5.0, 6.0)),
        Pair("Sugar-Free Diet Yogurt", Triple(80.0, 12.0, 0.0))
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val selBitmap = BitmapFactory.decodeStream(inputStream)
                bitmapToScan = selBitmap
                viewModel.resetScanState() // clear past results
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ScannerScreen", "Image parse error", e)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Scanned Food Label Lab",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C1E),
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Scans nutritional details from food label images using Gemini multimodal classification.",
                fontSize = 13.sp,
                color = Color(0xFF44474E)
            )
        }

        // Image display and scan action card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmapToScan != null) {
                        Image(
                            bitmap = bitmapToScan!!.asImageBitmap(),
                            contentDescription = "Image to scan",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE1E2EC)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Empty box with instructions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3F4F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Browse label",
                                    tint = Color(0xFF44474E),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Select Nutritional Label Photo",
                                    color = Color(0xFF191C1E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pick an image from your device storage or select a direct test preset below.",
                                    color = Color(0xFF44474E),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDDE3EA),
                                contentColor = Color(0xFF001D36)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Custom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (bitmapToScan != null) {
                            Button(
                                onClick = { viewModel.analyzeFoodLabel(bitmapToScan!!) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0061A4),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = scanUiState !is ScanUiState.Analyzing,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("analyze_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scan", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Preset Label Builder list (For instant emulator testing without local image files!)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant Testing Presets",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                    Text(
                        text = "Generates high-contrast dynamic nutrition label images directly in-memory to test Gemini OCR classification.",
                        fontSize = 11.sp,
                        color = Color(0xFF44474E)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (pName, pVals) ->
                            val isSelected = bitmapToScan != null && imageUri == null // just indicative

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    imageUri = null
                                    bitmapToScan = generateLabelBitmap(
                                        foodName = pName,
                                        calories = pVals.first,
                                        protein = pVals.second,
                                        fiber = pVals.third
                                    )
                                    viewModel.resetScanState()
                                    Toast.makeText(context, "$pName generated!", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(pName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF3F4F9),
                                    labelColor = Color(0xFF44474E),
                                    selectedContainerColor = Color(0xFFD1E4FF),
                                    selectedLabelColor = Color(0xFF001D36)
                                ),
                                border = null,
                                modifier = Modifier.testTag("preset_$pName")
                            )
                        }
                    }
                }
            }
        }

        // Analysis Loading or Results Screen
        item {
            AnimatedVisibility(
                visible = scanUiState !is ScanUiState.Idle,
                enter = expandIn(expandFrom = Alignment.TopCenter) + fadeIn(),
                exit = shrinkOut(shrinkTowards = Alignment.TopCenter) + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        when (val state = scanUiState) {
                            ScanUiState.Analyzing -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF0061A4))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "GEMINI IS ANALYZING THE LABEL...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0061A4),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Reading product title & micro/macronutrients with multimodal text layout analysis...",
                                        fontSize = 11.sp,
                                        color = Color(0xFF44474E),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            is ScanUiState.Success -> {
                                var editedName by remember(state) { mutableStateOf(state.foodName) }
                                var editedCalories by remember(state) { mutableStateOf(state.calories.toString()) }
                                var editedProtein by remember(state) { mutableStateOf(state.protein.toString()) }
                                var editedFiber by remember(state) { mutableStateOf(state.fiber.toString()) }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.TaskAlt, contentDescription = "Scanned OK", tint = Color(0xFF0061A4))
                                        Text(
                                            text = "Extracted Nutrition Data",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF191C1E)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Below is the classified data. Verify and adjust values customly before saving.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF44474E)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Fields grid
                                    OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        label = { Text("Product/Food Name") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0061A4),
                                            focusedLabelColor = Color(0xFF0061A4),
                                            unfocusedBorderColor = Color(0xFFC4C6D0),
                                            unfocusedTextColor = Color(0xFF191C1E),
                                            focusedTextColor = Color(0xFF191C1E)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedCalories,
                                            onValueChange = { editedCalories = it },
                                            label = { Text("Calories (kcal)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF0061A4),
                                                focusedLabelColor = Color(0xFF0061A4),
                                                unfocusedBorderColor = Color(0xFFC4C6D0),
                                                unfocusedTextColor = Color(0xFF191C1E),
                                                focusedTextColor = Color(0xFF191C1E)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = editedProtein,
                                            onValueChange = { editedProtein = it },
                                            label = { Text("Protein (g)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF0061A4),
                                                focusedLabelColor = Color(0xFF0061A4),
                                                unfocusedBorderColor = Color(0xFFC4C6D0),
                                                unfocusedTextColor = Color(0xFF191C1E),
                                                focusedTextColor = Color(0xFF191C1E)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = editedFiber,
                                            onValueChange = { editedFiber = it },
                                            label = { Text("Fiber (g)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF0061A4),
                                                focusedLabelColor = Color(0xFF0061A4),
                                                unfocusedBorderColor = Color(0xFFC4C6D0),
                                                unfocusedTextColor = Color(0xFF191C1E),
                                                focusedTextColor = Color(0xFF191C1E)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            viewModel.addIntake(
                                                name = editedName,
                                                calories = editedCalories.toDoubleOrNull() ?: 0.0,
                                                protein = editedProtein.toDoubleOrNull() ?: 0.0,
                                                fiber = editedFiber.toDoubleOrNull() ?: 0.0
                                            )
                                            viewModel.resetScanState()
                                            bitmapToScan = null
                                            Toast.makeText(context, "Log recorded successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0061A4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_scanned_btn")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save Intake", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Accept & Log to Diary", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is ScanUiState.Error -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Error, contentDescription = "Scan Error", tint = Color.Red, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = state.message,
                                        color = Color(0xFF191C1E),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.resetScanState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE3EA), contentColor = Color(0xFF001D36))
                                    ) {
                                        Text("Dismiss", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// ======================== DEFICIENCIES SCREEN ========================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeficienciesScreen(viewModel: NutriViewModel) {
    val context = LocalContext.current
    val deficiencyUiState by viewModel.deficiencyUiState.collectAsState()
    val allDeficiencyAnalyses by viewModel.allDeficiencyAnalyses.collectAsState()

    var textInputSymptom by remember { mutableStateOf("") }
    val symptomsList = listOf(
        "Severe Fatigue",
        "Dry skin & brittle hair",
        "Persistent muscle twitching",
        "Bleeding gums after brushing",
        "Joint stiffness & bone pain",
        "Mouth ulcers / cracks",
        "Slow healing cuts",
        "Weak night vision",
        "Cold hands or feet"
    )
    val selectedSymptoms = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Vitamin Deficiency Analyzer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C1E),
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Select your symptoms and let AI identify potential deficiencies and suggest rich nutrient intake.",
                fontSize = 13.sp,
                color = Color(0xFF44474E)
            )
        }

        // Checklist of symptoms card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Select All Symptoms That Apply:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        symptomsList.forEach { symptom ->
                            val isChecked = selectedSymptoms.contains(symptom)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) {
                                        selectedSymptoms.remove(symptom)
                                    } else {
                                        selectedSymptoms.add(symptom)
                                    }
                                },
                                label = { Text(symptom, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF3F4F9),
                                    labelColor = Color(0xFF44474E),
                                    selectedContainerColor = Color(0xFFD1E4FF),
                                    selectedLabelColor = Color(0xFF001D36)
                                ),
                                border = null,
                                modifier = Modifier.testTag("symptom_$symptom")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. Add any other physical symptoms / comments:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = textInputSymptom,
                        onValueChange = { textInputSymptom = it },
                        placeholder = { Text("E.g. Frequent leg cramps at night, general weakness...", color = Color(0xFF44474E)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0061A4),
                            focusedLabelColor = Color(0xFF0061A4),
                            unfocusedBorderColor = Color(0xFFC4C6D0),
                            focusedTextColor = Color(0xFF191C1E),
                            unfocusedTextColor = Color(0xFF191C1E)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedSymptoms.isEmpty() && textInputSymptom.isBlank()) {
                                Toast.makeText(context, "Please select at least 1 symptom or enter a custom comment.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.analyzeDeficiencies(selectedSymptoms.toList(), textInputSymptom)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = deficiencyUiState !is DeficiencyUiState.Analyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_deficiencies_btn")
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = "Evaluate", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Deficiencies", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Analysis Result Pane
        item {
            AnimatedVisibility(
                visible = deficiencyUiState !is DeficiencyUiState.Idle,
                enter = expandIn(expandFrom = Alignment.TopCenter) + fadeIn(),
                exit = shrinkOut(shrinkTowards = Alignment.TopCenter) + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        when (val state = deficiencyUiState) {
                            DeficiencyUiState.Analyzing -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF0061A4))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "GEMINI CLINICAL ADVISOR EVALUATING...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0061A4),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Mapping clinical correlations to trace mineral deficiencies & foods recommendations...",
                                        fontSize = 11.sp,
                                        color = Color(0xFF44474E),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            is DeficiencyUiState.Success -> {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.TipsAndUpdates, contentDescription = "Parsed", tint = Color(0xFF0061A4))
                                        Text(
                                            text = "Clinical Deficiency Predictions",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF191C1E)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Suggested potential deficiencies based on symptomatology. Consult a doctor for diagnostic blood logs.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF44474E)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    state.deficiencies.forEach { item ->
                                        DeficiencyCardView(item)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.resetDeficiencyState()
                                            selectedSymptoms.clear()
                                            textInputSymptom = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE3EA), contentColor = Color(0xFF001D36)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Finished / Done", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is DeficiencyUiState.Error -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = state.message,
                                        color = Color(0xFF191C1E),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.resetDeficiencyState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE3EA), contentColor = Color(0xFF001D36))
                                    ) {
                                        Text("Dismiss", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Deficiency Analyses Historical List
        if (allDeficiencyAnalyses.isNotEmpty()) {
            item {
                Text(
                    text = "Past Deficiency Evaluations (${allDeficiencyAnalyses.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1E),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(allDeficiencyAnalyses) { analysis ->
                HistoricalAnalysisCard(analysis = analysis, onDelete = { viewModel.deleteAnalysis(analysis) })
            }
        }
    }
}

@Composable
fun DeficiencyCardView(item: DeficiencyItem) {
    val confidenceColor = when (item.confidence.lowercase()) {
        "high" -> Color(0xFFBA1A1A) // High contrast M3 red
        "medium" -> Color(0xFF8B5000) // Dark orange
        else -> Color(0xFF006D3A) // Dark Green
    }
    val cardBg = when (item.confidence.lowercase()) {
        "high" -> Color(0xFFFFDAD6) // light pastel red
        "medium" -> Color(0xFFFFDDB3) // light pastel orange
        else -> Color(0xFFD8E6C8) // light pastel green
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.vitaminName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1E)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(confidenceColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.confidence} Match",
                        color = confidenceColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.symptomLink,
                fontSize = 12.sp,
                color = Color(0xFF44474E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Foods block
            Text(
                text = "Target Foods to Intake:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF001D36)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.recommendedFoods.forEach { food ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFlorist,
                            contentDescription = "Food suggestion",
                            tint = Color(0xFF006D3A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(food, fontSize = 12.sp, color = Color(0xFF191C1E))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalAnalysisCard(analysis: DeficiencyAnalysis, onDelete: () -> Unit) {
    // Deserialize past predictedDeficiencies list using Moshi
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(analysis.timestamp) {
        val date = Date(analysis.timestamp)
        val format = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        format.format(date)
    }

    val deficienciesList = remember(analysis.predictedDeficiencies) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, DeficiencyItem::class.java)
            val adapter = moshi.adapter<List<DeficiencyItem>>(type)
            adapter.fromJson(analysis.predictedDeficiencies) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("history_analysis_${analysis.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE6DEFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Symptom Logs",
                            tint = Color(0xFF625B71),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Analysis - ${analysis.symptoms.take(28)}${if (analysis.symptoms.length > 28) "..." else ""}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E),
                            fontSize = 14.sp
                        )
                        Text(
                            text = formattedDate,
                            color = Color(0xFF44474E),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color(0xFF44474E)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFDDE3EA))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Symptoms Analyzed:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474E),
                        fontSize = 11.sp
                    )
                    Text(
                        text = analysis.symptoms,
                        color = Color(0xFF191C1E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Predicted Deficiencies:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (deficienciesList.isEmpty()) {
                        Text("No deficiencies extracted.", color = Color(0xFF191C1E), fontSize = 12.sp)
                    } else {
                        deficienciesList.forEach { value ->
                            DeficiencyCardView(item = value)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ======================== DIALOGS & MANUAL ADD INPUTS ========================

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    viewModel: NutriViewModel,
    onAdd: (String, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var vitamins by remember { mutableStateOf("") }

    var aiInputText by remember { mutableStateOf("") }
    val aiState by viewModel.aiCalculateUiState.collectAsState()

    // Reset state on start
    LaunchedEffect(Unit) {
        viewModel.resetAiCalculateState()
    }

    // Auto fill properties upon successful AI parsing
    LaunchedEffect(aiState) {
        if (aiState is com.example.ui.viewmodel.AiCalculateUiState.Success) {
            val success = aiState as com.example.ui.viewmodel.AiCalculateUiState.Success
            name = success.foodName
            calories = success.calories.toInt().toString()
            protein = success.protein.toString()
            fiber = success.fiber.toString()
            vitamins = success.vitamins
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = "Manual Intake", tint = Color(0xFF0061A4))
                    Text(text = "Log Food Intake", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1E))
                }

                Text(
                    text = "Estimate nutrients automatically with our AI calculator, or enter them manually.",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E)
                )

                // AI Autocomplete Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = Color(0xFF0061A4), modifier = Modifier.size(16.dp))
                            Text(text = "AI Nutrient Autocomplete (Gemini)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001D36))
                        }
                        
                        Text(
                            text = "Describe your food (e.g. \"3 slices of brown bread with peanut butter and a glass of milk\"). We'll calculate proteins, fiber, vitamins, and calories!",
                            fontSize = 11.sp,
                            color = Color(0xFF44474E)
                        )

                        OutlinedTextField(
                            value = aiInputText,
                            onValueChange = { aiInputText = it },
                            placeholder = { Text("E.g. A bowl of oatmeal with banana and honey", fontSize = 12.sp) },
                            singleLine = false,
                            maxLines = 2,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC4C6D0),
                                focusedTextColor = Color(0xFF191C1E),
                                unfocusedTextColor = Color(0xFF191C1E),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (aiState is com.example.ui.viewmodel.AiCalculateUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0061A4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI is thinking...", fontSize = 11.sp, color = Color(0xFF0061A4), fontWeight = FontWeight.Medium)
                            } else {
                                Button(
                                    onClick = {
                                        if (aiInputText.isNotBlank()) {
                                            viewModel.calculateNutrientsFromText(aiInputText)
                                        }
                                    },
                                    enabled = aiInputText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = "Calculate", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze with AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        when (aiState) {
                            is com.example.ui.viewmodel.AiCalculateUiState.Success -> {
                                Text(
                                    text = "✅ Autocomplete filled! Review and adjust values below.",
                                    color = Color(0xFF027A48),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is com.example.ui.viewmodel.AiCalculateUiState.Error -> {
                                Text(
                                    text = "❌ ${(aiState as com.example.ui.viewmodel.AiCalculateUiState.Error).message}",
                                    color = Color(0xFFBA1A1A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            else -> {}
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFDDE3EA), modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name") },
                    placeholder = { Text("E.g. Grilled Chicken Salad") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_name_tf")
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_cal_tf")
                )

                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_protein_tf")
                )

                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text("Fiber (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_fiber_tf")
                )

                OutlinedTextField(
                    value = vitamins,
                    onValueChange = { vitamins = it },
                    label = { Text("Primary Vitamins & Minerals") },
                    placeholder = { Text("E.g. Vitamin C, Calcium, Zinc") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_vitamins_tf")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(
                                    name,
                                    calories.toDoubleOrNull() ?: 0.0,
                                    protein.toDoubleOrNull() ?: 0.0,
                                    fiber.toDoubleOrNull() ?: 0.0,
                                    vitamins
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("manual_add_save_btn")
                    ) {
                        Text("Add to Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TargetInputDialog(
    currentCalories: Double,
    currentProtein: Double,
    currentFiber: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double) -> Unit
) {
    var calories by remember { mutableStateOf(currentCalories.toInt().toString()) }
    var protein by remember { mutableStateOf(currentProtein.toInt().toString()) }
    var fiber by remember { mutableStateOf(currentFiber.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Edit Targets", tint = Color(0xFF0061A4))
                    Text(text = "Edit Daily Targets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1E))
                }

                Text(
                    text = "Set daily target standards for tracking macro progress.",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E)
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories Target (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("target_cal_tf")
                )

                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("target_prot_tf")
                )

                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text("Fiber Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        unfocusedTextColor = Color(0xFF191C1E),
                        focusedTextColor = Color(0xFF191C1E)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("target_fiber_tf")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                calories.toDoubleOrNull() ?: currentCalories,
                                protein.toDoubleOrNull() ?: currentProtein,
                                fiber.toDoubleOrNull() ?: currentFiber
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("target_save_btn")
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeLoginScreen(viewModel: NutriViewModel) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFDDE3EA)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Header Section
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFD1E4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "NutriTrack Logo",
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome to NutriTrack",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Smart Nutrient & Deficiency Tracker",
                        fontSize = 12.sp,
                        color = Color(0xFF44474E),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sign in using your Google Email Address to securely synchronize maps, logs, and auto-retrieve cloud Gemini API credentials.",
                        fontSize = 11.sp,
                        color = Color(0xFF44474E),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Email Address") },
                        placeholder = { Text("username@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0061A4),
                            unfocusedBorderColor = Color(0xFFC4C6D0),
                            unfocusedTextColor = Color(0xFF191C1E),
                            focusedTextColor = Color(0xFF191C1E),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("login_email_tf")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Keys info",
                            tint = Color(0xFF027A48),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto-linking cloud Gemini Workspace to your profile.",
                            fontSize = 10.sp,
                            color = Color(0xFF027A48),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isAuthenticating) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF0061A4))
                            Text(
                                text = "Authorizing credentials & retrieving token...",
                                fontSize = 11.sp,
                                color = Color(0xFF0061A4),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val emailToUse = emailInput.trim().ifEmpty { "cteja141@gmail.com" }
                                isAuthenticating = true
                                Toast.makeText(context, "Redirecting to Google Accounts secure sync...", Toast.LENGTH_SHORT).show()
                                viewModel.loginWithGoogle(emailToUse)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_login_submit_btn")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Google sign-in", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Text(
                        text = "💡 Tap 'Continue' to directly log in and map keys.",
                        color = Color(0xFF0061A4),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
