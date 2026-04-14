package com.zmstore.projectr.ui.login

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.R
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.MedicleanDarkGreen
import com.zmstore.projectr.ui.theme.MedicleanMint
import com.zmstore.projectr.ui.theme.MedicleanTeal
import com.zmstore.projectr.ui.theme.MedicleanWhite
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var showEmailLogin by remember { mutableStateOf(false) }
    var showPhoneLogin by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val webClientId = stringResource(id = R.string.google_web_client_id)

    val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            viewModel.signInWithPhoneCredential(credential) { success ->
                isLoading = false
                if (success) onNavigateToHome()
                else errorMessage = context.getString(R.string.login_error_phone)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            isLoading = false
            errorMessage = e.localizedMessage ?: context.getString(R.string.login_error_phone)
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            isLoading = false
            verificationId = id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MedicleanWhite, MedicleanMint)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.iconeapp),
                        contentDescription = stringResource(id = R.string.login_logo_desc),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.login_welcome),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MedicleanDarkGreen,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!showEmailLogin && !showPhoneLogin) {
                // Google Login Button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        viewModel.signInWithGoogle(context as Activity, webClientId) { success ->
                            isLoading = false
                            if (success) onNavigateToHome()
                            else errorMessage = "Erro na autenticação Google. Verifique se o seu SHA-1 (Debug e Release) foi adicionado corretamente ao Console do Firebase."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MedicleanDarkGreen
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    enabled = !isLoading
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MedicleanTeal
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(id = R.string.login_google_btn), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Email Option Button
                OutlinedButton(
                    onClick = { showEmailLogin = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicleanDarkGreen),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.login_email_btn), fontWeight = FontWeight.Bold)
                }
            } else if (showPhoneLogin) {
                // Phone Auth UI
                Text(
                    text = stringResource(R.string.login_phone_btn),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MedicleanDarkGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (verificationId == null) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(stringResource(R.string.login_phone_label)) },
                        placeholder = { Text(stringResource(R.string.login_phone_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank() && phoneNumber.length >= 10) {
                                isLoading = true
                                errorMessage = null
                                viewModel.verifyPhoneNumber(phoneNumber, context as Activity, phoneAuthCallbacks)
                            } else {
                                errorMessage = context.getString(R.string.login_error_phone_invalid)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.login_phone_send_otp), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text(stringResource(R.string.login_phone_otp_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (otpCode.length == 6) {
                                isLoading = true
                                val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
                                viewModel.signInWithPhoneCredential(credential) { success ->
                                    isLoading = false
                                    if (success) onNavigateToHome()
                                    else errorMessage = context.getString(R.string.login_error_otp_invalid)
                                }
                            } else {
                                errorMessage = context.getString(R.string.login_error_otp_invalid)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.login_phone_verify_otp), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                TextButton(onClick = { 
                    showPhoneLogin = false
                    verificationId = null
                    otpCode = ""
                }) {
                    Text(stringResource(id = R.string.login_back), color = MedicleanDarkGreen.copy(alpha = 0.6f))
                }
            } else {
                // Email Login Fields
                Text(
                    text = if (isRegistering) stringResource(R.string.login_register_btn) else stringResource(R.string.login_email_btn),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MedicleanDarkGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.login_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MedicleanDarkGreen,
                        unfocusedTextColor = MedicleanDarkGreen,
                        focusedBorderColor = MedicleanTeal,
                        unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f),
                        focusedLabelColor = MedicleanTeal,
                        unfocusedLabelColor = MedicleanDarkGreen.copy(alpha = 0.6f)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(id = R.string.login_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MedicleanDarkGreen,
                        unfocusedTextColor = MedicleanDarkGreen,
                        focusedBorderColor = MedicleanTeal,
                        unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f),
                        focusedLabelColor = MedicleanTeal,
                        unfocusedLabelColor = MedicleanDarkGreen.copy(alpha = 0.6f)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (email.isNotBlank() && email.contains("@") && password.length >= 6) {
                            isLoading = true
                            errorMessage = null
                            if (isRegistering) {
                                viewModel.signUpWithEmail(email, password) { success ->
                                    isLoading = false
                                    if (success) onNavigateToHome()
                                    else errorMessage = "Erro ao criar conta. Tente outro email."
                                }
                            } else {
                                viewModel.signInWithEmail(email, password) { success ->
                                    isLoading = false
                                    if (success) onNavigateToHome()
                                    else errorMessage = context.getString(R.string.login_error_email)
                                }
                            }
                        } else {
                            errorMessage = if (password.length < 6) 
                                context.getString(R.string.login_error_password)
                            else 
                                context.getString(R.string.login_error_invalid_email)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                    enabled = !isLoading
                ) {
                    Text(
                        if (isRegistering) stringResource(id = R.string.login_register_btn) else stringResource(id = R.string.login_continue), 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { isRegistering = !isRegistering }) {
                    Text(
                        if (isRegistering) stringResource(R.string.login_signin_link) else stringResource(R.string.login_register_link),
                        color = MedicleanTeal
                    )
                }
                
                TextButton(onClick = { 
                    showEmailLogin = false
                    isRegistering = false
                    password = ""
                }) {
                    Text(stringResource(id = R.string.login_back), color = MedicleanDarkGreen.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guest Login Button
            TextButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    viewModel.signInAnonymously { success ->
                        isLoading = false
                        if (success) onNavigateToHome()
                        else errorMessage = "Erro ao entrar como convidado"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    stringResource(id = R.string.login_guest_btn),
                    fontWeight = FontWeight.Medium,
                    color = MedicleanTeal
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = MedicleanTeal)
            }
        }
    }
}
