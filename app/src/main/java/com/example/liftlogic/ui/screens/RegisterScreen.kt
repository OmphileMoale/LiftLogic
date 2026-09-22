package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel

@Composable
fun RegisterScreen(
    viewModel: AppViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("kg") }
    var localError by remember { mutableStateOf<String?>(null) }
    val error by viewModel.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.title_create_account), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.label_full_name)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            email, { email = it },
            label = { Text(stringResource(R.string.label_email_address)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it },
            label = { Text(stringResource(R.string.label_password_min)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            confirmPassword, { confirmPassword = it },
            label = { Text(stringResource(R.string.label_confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.label_preferred_units), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChipUnit("kg", stringResource(R.string.unit_kg), units) { units = "kg" }
            Spacer(Modifier.width(8.dp))
            FilterChipUnit("lb", stringResource(R.string.unit_lb), units) { units = "lb" }
        }

        val passwordsDontMatch = stringResource(R.string.error_passwords_dont_match)
        val shownError = localError ?: error?.asMessage()
        if (shownError != null) {
            Spacer(Modifier.height(12.dp))
            Text(shownError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                localError = null
                if (password != confirmPassword) {
                    localError = passwordsDontMatch
                } else {
                    viewModel.register(name, email, password, units, onRegisterSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_create_account)) }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.prompt_have_account))
        }
    }
}

@Composable
private fun FilterChipUnit(key: String, label: String, selectedKey: String, onClick: () -> Unit) {
    FilterChip(selected = selectedKey == key, onClick = onClick, label = { Text(label) })
}