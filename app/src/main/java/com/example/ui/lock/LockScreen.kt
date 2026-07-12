package com.example.ui.lock

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun LockScreen(
    uiState: LockScreenUiState,
    onPinEntered: (String) -> Unit,
    onPatternEntered: (List<Int>) -> Unit,
    onRecoveryAnswered: (String) -> Unit,
    onToggleRecovery: (Boolean) -> Unit,
    onBiometricTriggered: () -> Unit,
    onExitClicked: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lock_screen_surface"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Locked App Details
            LockedAppHeader(
                appLabel = uiState.appLabel,
                appIcon = uiState.appIcon
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLockedOut -> {
                        LockoutView(
                            remainingSeconds = uiState.lockoutRemainingSeconds
                        )
                    }
                    uiState.isRecoveryMode -> {
                        RecoveryView(
                            question = uiState.securityQuestion,
                            recoveryError = uiState.recoveryError,
                            recoverySuccess = uiState.recoverySuccess,
                            onRecoveryAnswered = onRecoveryAnswered,
                            onCancel = { onToggleRecovery(false) }
                        )
                    }
                    else -> {
                        if (uiState.passwordType == "PIN") {
                            PinLockView(
                                onPinEntered = onPinEntered,
                                onForgotClick = { onToggleRecovery(true) },
                                biometricAvailable = uiState.biometricAvailable,
                                onBiometricClick = onBiometricTriggered,
                                failedAttempts = uiState.failedAttempts
                            )
                        } else {
                            PatternLockView(
                                onPatternEntered = { path ->
                                    onPatternEntered(path)
                                },
                                onForgotClick = { onToggleRecovery(true) },
                                failedAttempts = uiState.failedAttempts
                            )
                        }
                    }
                }
            }

            // Exit Button to prevent system bypass
            Button(
                onClick = onExitClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("lock_exit_button")
            ) {
                Text(
                    text = "Close Application",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun LockedAppHeader(
    appLabel: String,
    appIcon: Drawable?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (appIcon != null) {
                val bitmap = remember(appIcon) { appIcon.toBitmap().asImageBitmap() }
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawImage(bitmap, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Aegis AppLock",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun PinLockView(
    onPinEntered: (String) -> Unit,
    onForgotClick: () -> Unit,
    biometricAvailable: Boolean,
    onBiometricClick: () -> Unit,
    failedAttempts: Int
) {
    var input by remember { mutableStateOf("") }
    val maxPinLength = 4

    // Monitor input length
    LaunchedEffect(input) {
        if (input.length == maxPinLength) {
            onPinEntered(input)
            input = "" // reset input on check
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter Security PIN",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        if (failedAttempts > 0) {
            Text(
                text = "Failed attempts: $failedAttempts/5",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Circles indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until maxPinLength) {
                val filled = i < input.length
                val size by animateDpAsState(
                    targetValue = if (filled) 20.dp else 16.dp,
                    animationSpec = spring()
                )
                val color by animateColorAsState(
                    targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(color, CircleShape)
                )
            }
        }

        // Numeric Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("FORGOT", "0", "DELETE")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "FORGOT" -> {
                                    TextButton(
                                        onClick = onForgotClick,
                                        modifier = Modifier.testTag("forgot_pin_button")
                                    ) {
                                        Text(
                                            text = "Forgot?",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                "DELETE" -> {
                                    IconButton(
                                        onClick = { if (input.isNotEmpty()) input = input.dropLast(1) },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                ), CircleShape
                                            )
                                            .testTag("pin_delete_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                            .clickable {
                                                if (input.length < maxPinLength) {
                                                    input += key
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("biometric_quick_auth_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PatternLockView(
    onPatternEntered: (List<Int>) -> Unit,
    onForgotClick: () -> Unit,
    failedAttempts: Int
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val nodeRadius = 14.dp
    val hitRadiusPx = 60f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Draw Security Pattern",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        if (failedAttempts > 0) {
            Text(
                text = "Failed attempts: $failedAttempts/5",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val dotActiveColor = MaterialTheme.colorScheme.primary
        val dotInactiveColor = MaterialTheme.colorScheme.outlineVariant
        val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

        BoxWithConstraints(
            modifier = Modifier
                .size(280.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            currentTouchPoint = offset
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextPoint = currentTouchPoint?.plus(dragAmount) ?: change.position
                            currentTouchPoint = nextPoint

                            // Size of canvas inside BoxWithConstraints
                            val widthPx = size.width.toFloat()
                            val stepX = widthPx / 4f
                            val stepY = widthPx / 4f

                            for (row in 0..2) {
                                for (col in 0..2) {
                                    val index = row * 3 + col
                                    val nodeX = stepX * (col + 1)
                                    val nodeY = stepY * (row + 1)

                                    val distance = sqrt(
                                        (nextPoint.x - nodeX).pow(2) + (nextPoint.y - nodeY).pow(2)
                                    )
                                    if (distance < hitRadiusPx && index !in selectedNodes) {
                                        selectedNodes.add(index)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (selectedNodes.isNotEmpty()) {
                                onPatternEntered(selectedNodes.toList())
                            }
                            selectedNodes.clear()
                            currentTouchPoint = null
                        },
                        onDragCancel = {
                            selectedNodes.clear()
                            currentTouchPoint = null
                        }
                    )
                }
                .testTag("pattern_grid_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 4f
                val stepY = size.height / 4f

                // Draw lines between selected nodes
                if (selectedNodes.size > 1) {
                    for (i in 0 until selectedNodes.size - 1) {
                        val node1 = selectedNodes[i]
                        val node2 = selectedNodes[i + 1]

                        val n1X = stepX * ((node1 % 3) + 1)
                        val n1Y = stepY * ((node1 / 3) + 1)

                        val n2X = stepX * ((node2 % 3) + 1)
                        val n2Y = stepY * ((node2 / 3) + 1)

                        drawLine(
                            color = lineColor,
                            start = Offset(n1X, n1Y),
                            end = Offset(n2X, n2Y),
                            strokeWidth = 8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw line to current touch point
                if (selectedNodes.isNotEmpty() && currentTouchPoint != null) {
                    val lastNode = selectedNodes.last()
                    val lastX = stepX * ((lastNode % 3) + 1)
                    val lastY = stepY * ((lastNode / 3) + 1)

                    drawLine(
                        color = lineColor,
                        start = Offset(lastX, lastY),
                        end = currentTouchPoint!!,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw dots
                for (row in 0..2) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val nodeX = stepX * (col + 1)
                        val nodeY = stepY * (row + 1)
                        val active = index in selectedNodes

                        // Outer glowing circle
                        if (active) {
                            drawCircle(
                                color = dotActiveColor.copy(alpha = 0.2f),
                                radius = nodeRadius.toPx() * 1.8f,
                                center = Offset(nodeX, nodeY)
                            )
                        }

                        // Inner dot
                        drawCircle(
                            color = if (active) dotActiveColor else dotInactiveColor,
                            radius = if (active) nodeRadius.toPx() * 0.6f else nodeRadius.toPx() * 0.4f,
                            center = Offset(nodeX, nodeY)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onForgotClick,
            modifier = Modifier.testTag("forgot_pattern_button")
        ) {
            Text(
                text = "Forgot Pattern?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LockoutView(remainingSeconds: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked Out",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Too Many Failed Attempts",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aegis AppLock is temporarily suspended for security protection. Please wait before attempting to unlock again.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Try again in ${remainingSeconds}s",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecoveryView(
    question: String,
    recoveryError: Boolean,
    recoverySuccess: Boolean,
    onRecoveryAnswered: (String) -> Unit,
    onCancel: () -> Unit
) {
    var answer by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Recovery icon",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Password Recovery",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Answer your secret security question locally to bypass and reset AppLock settings.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = question,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Your Secret Answer") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_answer_input"),
            shape = RoundedCornerShape(12.dp),
            isError = recoveryError,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (recoveryError) {
            Text(
                text = "The answer is incorrect. Please check spelling and try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Start)
            )
        }

        if (recoverySuccess) {
            Text(
                text = "Recovery Successful! Resetting security data...",
                color = Color.Green,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = { onRecoveryAnswered(answer) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("recovery_submit_button"),
                enabled = answer.isNotBlank() && !recoverySuccess
            ) {
                Text("Verify Answer")
            }
        }
    }
}
