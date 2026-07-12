package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AppLockAccessibilityService
import com.example.utils.ServiceUtils

@Composable
fun DashboardScreen(
    lockedAppsCount: Int,
    isAppLockEnabled: Boolean,
    onToggleGlobalLock: (Boolean) -> Unit,
    onNavigateToAppSelection: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var isServiceActive by remember {
        mutableStateOf(ServiceUtils.isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java))
    }

    // Refresh service status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isServiceActive = ServiceUtils.isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)
            kotlinx.coroutines.delay(2000)
        }
    }

    val overallSecure = isAppLockEnabled && isServiceActive

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Aegis",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Fortress Lock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Security Status Banner Card (Geometric Hero)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_status_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (overallSecure) MaterialTheme.colorScheme.primaryContainer 
                                 else MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
            ) {
                // Background Geometric Shapes
                val decorationColor = if (overallSecure) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                }

                // Top right shape
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 36.dp, y = (-36).dp)
                        .size(160.dp)
                        .background(decorationColor, CircleShape)
                )

                // Bottom left shape
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-24).dp, y = 24.dp)
                        .size(100.dp)
                        .background(decorationColor, CircleShape)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Elevated status icon with concentric circles
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (overallSecure) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.error, 
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (overallSecure) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Status",
                                tint = if (overallSecure) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (overallSecure) "Protection Active" else "Attention Required",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overallSecure) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (overallSecure) "ALL SERVICES RUNNING" else "ACTION REQUIRED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (overallSecure) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer).copy(alpha = 0.6f),
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            !isAppLockEnabled -> "AppLock global protection is manually paused. Turn it back on below."
                            !isServiceActive -> "Accessibility Service is currently INACTIVE. Click to enable in Settings."
                            else -> "Your credentials are encrypted locally, and $lockedAppsCount applications are secured."
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (overallSecure) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                                else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )

                    if (!isServiceActive) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Enable Service", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grid Menu Options
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats & Manage Card
            Card(
                onClick = onNavigateToAppSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dashboard_locked_apps_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AppRegistration,
                            contentDescription = "Applications",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Protect Applications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage which system and installed apps are protected by AppLock",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = lockedAppsCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Global Lock Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dashboard_toggle_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isAppLockEnabled) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Global Protection Toggle",
                            tint = if (isAppLockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isAppLockEnabled) "AppLock is active" else "AppLock is paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Instantly toggle global overlay lock protection on or off",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onToggleGlobalLock(!isAppLockEnabled) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAppLockEnabled) MaterialTheme.colorScheme.error 
                                             else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dashboard_toggle_button")
                    ) {
                        Text(
                            text = if (isAppLockEnabled) "Pause" else "Resume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Advanced Settings Card
            Card(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dashboard_settings_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Advanced Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Theme, backups, activity logs, and options",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

