package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AppLockAccessibilityService
import com.example.ui.onboarding.OnboardingUiState
import com.example.ui.onboarding.OnboardingViewModel
import com.example.utils.ServiceUtils

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    uiState: OnboardingUiState,
    onOnboardingFinished: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Listen for completion
    LaunchedEffect(uiState.onboardingComplete) {
        if (uiState.onboardingComplete) {
            onOnboardingFinished()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.currentStep > 0 && uiState.currentStep < 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousStep() },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Setup AppLock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Progress dots
            if (uiState.currentStep < 5) {
                StepIndicator(currentStep = uiState.currentStep, totalSteps = 5)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.currentStep) {
                    0 -> IntroStep(onNext = { viewModel.nextStep() })
                    1 -> LockTypeStep(onTypeSelected = { type -> viewModel.selectPasswordType(type) })
                    2 -> CreatePasswordStep(
                        passwordType = uiState.passwordType,
                        errorMsg = uiState.passwordErrorMsg,
                        onPasswordSaved = { pass -> viewModel.confirmPasswordAndNext(pass) }
                    )
                    3 -> SecurityQuestionStep(
                        questions = viewModel.securityQuestions,
                        errorMsg = uiState.securityErrorMsg,
                        onSaved = { idx, ans -> viewModel.setSecurityQuestionDetails(idx, ans) }
                    )
                    4 -> PermissionsStep(
                        onNext = { viewModel.nextStep() }
                    )
                    5 -> DoneStep(onDone = { viewModel.completeOnboarding() })
                }
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        for (i in 0 until totalSteps) {
            val active = i == currentStep
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
fun IntroStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield Security Logo",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Aegis AppLock",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A modern, open-source, offline-first security app locker. Aegis protects your applications from unauthorized eyes using locally encrypted credentials, and guarantees 100% data privacy with zero trackers or cloud logs.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_start_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Begin Setup",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun LockTypeStep(onTypeSelected: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Choose Authentication Method",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select your primary secure unlock method. You can also activate dynamic Fingerprint/Face authentication after setup.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            onClick = { onTypeSelected("PIN") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("select_pin_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = "PIN Lock",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Numeric PIN Lock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lock apps using a secure 4-digit code",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Card(
            onClick = { onTypeSelected("PATTERN") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("select_pattern_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pattern,
                        contentDescription = "Pattern Lock",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Pattern Lock Grid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lock apps drawing a custom 3x3 node path",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePasswordStep(
    passwordType: String,
    errorMsg: String?,
    onPasswordSaved: (String) -> Boolean
) {
    var pass1 by rememberSaveable { mutableStateOf("") }
    var pass2 by rememberSaveable { mutableStateOf("") }
    var stepConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = if (!stepConfirm) "Create Secure Password" else "Confirm Secure Password",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (passwordType == "PIN") {
                "Enter a 4-digit numeric code to protect your applications."
            } else {
                "Draw or construct a path code. To represent the pattern simply, enter any 4-9 digit sequence of numbers (e.g., 1234 or 01478)."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = if (!stepConfirm) pass1 else pass2,
            onValueChange = { input ->
                // enforce PIN limits
                if (passwordType == "PIN") {
                    if (input.all { it.isDigit() } && input.length <= 4) {
                        if (!stepConfirm) pass1 = input else pass2 = input
                    }
                } else {
                    if (!stepConfirm) pass1 = input else pass2 = input
                }
            },
            label = { Text(if (!stepConfirm) "Enter Security Key" else "Re-enter Security Key") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_pass_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = errorMsg != null
        )

        if (errorMsg != null) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!stepConfirm) {
                    if (pass1.length >= 4) {
                        stepConfirm = true
                    }
                } else {
                    val match = onPasswordSaved(pass2)
                    if (!match) {
                        // Reset confirmation
                        stepConfirm = false
                        pass1 = ""
                        pass2 = ""
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_save_password_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = if (!stepConfirm) pass1.length >= 4 else pass2.length >= 4
        ) {
            Text(if (!stepConfirm) "Continue" else "Save & Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionStep(
    questions: List<String>,
    errorMsg: String?,
    onSaved: (Int, String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Configure Local Recovery",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set up a recovery answer. If you forget your password, you can enter this local question answer to bypass and reset Aegis.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Questions Dropdown Box
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                readOnly = true,
                value = questions[selectedIndex],
                onValueChange = {},
                label = { Text("Recovery Question") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                questions.forEachIndexed { index, question ->
                    DropdownMenuItem(
                        text = { Text(question) },
                        onClick = {
                            selectedIndex = index
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Secret Answer (locally hashed)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_recovery_answer_input"),
            shape = RoundedCornerShape(12.dp),
            isError = errorMsg != null,
            singleLine = true
        )

        if (errorMsg != null) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSaved(selectedIndex, answer) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_save_recovery_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = answer.isNotBlank()
        ) {
            Text("Complete Recovery Config")
        }
    }
}

@Composable
fun PermissionsStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var isServiceEnabled by remember {
        mutableStateOf(ServiceUtils.isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java))
    }

    // Refresh state periodically or on active
    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled = ServiceUtils.isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)
            kotlinx.coroutines.delay(1500)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (isServiceEnabled) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant, 
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = "Permission icon",
                tint = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enable Accessibility Service",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To lock other applications, Android requires the Accessibility Service permission. This allows Aegis to inspect the foreground app's package name and display the secure lock overlay when needed.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) Color(0x1F4CAF50) else Color(0x1F2196F3)
            )
        ) {
            Text(
                text = if (isServiceEnabled) {
                    "✓ Status: Accessibility Service is ACTIVE. Aegis AppLock protection is operational."
                } else {
                    "ℹ Steps to Enable:\n1. Click 'Open System Settings' below.\n2. In list, choose 'Aegis AppLock'.\n3. Turn the service switch ON."
                },
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isServiceEnabled) Color(0xFF388E3C) else Color(0xFF1976D2),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isServiceEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_open_accessibility_button")
        ) {
            Text(if (isServiceEnabled) "Review Service Status" else "Open System Settings")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_permissions_next_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = isServiceEnabled
        ) {
            Text("Proceed to Complete")
        }
    }
}

@Composable
fun DoneStep(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFF2E7D32), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success icon",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You are fully protected!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Excellent! Setup is complete. Aegis is now armed and active. Open the dashboard to choose which applications you want to protect.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_complete_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Enter Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
