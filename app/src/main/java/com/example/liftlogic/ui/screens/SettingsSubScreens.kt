package com.example.liftlogic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.liftlogic.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun EditProfileScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    var name by remember(user) { mutableStateOf(user?.name ?: "") }

    SettingsSubPage("Edit Profile", onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { /* Implement actual update in viewModel if needed */ onBack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}

@Composable
fun EmailUsernameScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    var email by remember(user) { mutableStateOf(user?.email ?: "") }

    SettingsSubPage("Email / Username", onBack) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false // Usually email is primary key, handle with care
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Contact support to change your registered email address.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChangePasswordScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    SettingsSubPage("Change Password", onBack) {
        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onBack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Password")
        }
    }
}

@Composable
fun ProfilePictureScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    SettingsSubPage("Profile Picture", onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { /* Pick image logic */ }) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change Photo")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { /* Remove photo logic */ }) {
                Text("Remove Current Photo", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ThemeScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val colors = listOf(
        "Lift Green" to Color(0xFF2ECC71),
        "Ocean Blue" to Color(0xFF3498DB),
        "Royal Purple" to Color(0xFF9B59B6),
        "Sunset Orange" to Color(0xFFE67E22),
        "Crimson Red" to Color(0xFFE74C3C)
    )
    var selectedColor by remember { mutableStateOf(colors[0].second) }

    SettingsSubPage("App Theme", onBack) {
        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        colors.forEach { (name, color) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedColor = color }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Text(name, modifier = Modifier.weight(1f))
                RadioButton(selected = selectedColor == color, onClick = null)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Apply Theme")
        }
    }
}
