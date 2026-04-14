package com.zmstore.projectr.ui.login

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.zmstore.projectr.R
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*
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
            .background(Color.White)
    ) {
        // Decorative background elements
        if (!false) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-100).dp, y = (-100).dp)
                    .background(MedicleanTeal.copy(alpha = 0.05f), CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Premium Logo Container
            Surface(
                modifier = Modifier.size(130.dp),
                shape = RoundedCornerShape(38.dp),
                color = MedicleanWhite,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.iconeapp),
                        contentDescription = stringResource(id = R.string.login_logo_desc),
                        modifier = Modifier.size(95.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REMEDIO CERTO",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MedicleanDarkGreen,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Seu guia visual de saúde",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MedicleanDarkGreen.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

            if (errorMessage != null) {
                Surface(
                    color = MedicleanError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MedicleanError)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MedicleanError,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!showEmailLogin && !showPhoneLogin) {
                // Main Auth Options
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Google Login Button - Standard Premium
                    Surface(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            viewModel.signInWithGoogle(context as Activity, webClientId) { success ->
                                isLoading = false
                                if (success) onNavigateToHome()
                                else errorMessage = "Erro no Google Login (Verifique o SHA-1)."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MedicleanWhite,
                        shadowElevation = 4.dp,
                        enabled = !isLoading,
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MedicleanTeal
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Entrar com Google", 
                                fontWeight = FontWeight.Black,
                                color = MedicleanDarkGreen,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Email Login Button - Primary
                    Button(
                        onClick = { showEmailLogin = true },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("ENTRAR COM EMAIL", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Secondary Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showPhoneLogin = true }) {
                        Text("TELEFONE", color = MedicleanTeal, fontWeight = FontWeight.Black)
                    }
                    Box(modifier = Modifier.size(4.dp).background(Color.Gray.copy(alpha = 0.3f), CircleShape))
                    TextButton(onClick = {
                        isLoading = true
                        errorMessage = null
                        viewModel.signInAnonymously { success ->
                            isLoading = false
                            if (success) onNavigateToHome()
                            else errorMessage = "Erro no modo convidado"
                        }
                    }) {
                        Text("MODO VISITANTE", color = MedicleanDarkGreen.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
                    }
                }
            } else if (showPhoneLogin) {
                // Phone UI Layout
                AuthSection(
                    title = if(verificationId == null) "Acesso Celular" else "Verificação",
                    onBack = { 
                        showPhoneLogin = false
                        verificationId = null
                        otpCode = ""
                        errorMessage = null
                    }
                ) {
                    if (verificationId == null) {
                        PremiumTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = "Número com DDD",
                            icon = Icons.Default.Phone,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        PrimaryButton(
                            text = "RECEBER CÓDIGO",
                            isLoading = isLoading,
                            onClick = {
                                if (phoneNumber.isNotBlank() && phoneNumber.length >= 10) {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.verifyPhoneNumber(phoneNumber, context as Activity, phoneAuthCallbacks)
                                } else {
                                    errorMessage = "Digite um número válido"
                                }
                            }
                        )
                    } else {
                        PremiumTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = "Código SMS",
                            icon = Icons.Default.Lock,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        PrimaryButton(
                            text = "CONFIRMAR",
                            isLoading = isLoading,
                            onClick = {
                                if (otpCode.length == 6) {
                                    isLoading = true
                                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
                                    viewModel.signInWithPhoneCredential(credential) { success ->
                                        isLoading = false
                                        if (success) onNavigateToHome()
                                        else errorMessage = "Código inválido"
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // Email Auth Layout
                AuthSection(
                    title = if (isRegistering) "Criar conta" else "Bem-vindo",
                    onBack = { 
                        showEmailLogin = false
                        isRegistering = false
                        errorMessage = null
                    }
                ) {
                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Default.Email
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Senha",
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    PrimaryButton(
                        text = if (isRegistering) "CADASTRAR AGORA" else "ENTRAR",
                        isLoading = isLoading,
                        onClick = {
                            if (email.isNotBlank() && password.length >= 6) {
                                isLoading = true
                                errorMessage = null
                                if (isRegistering) {
                                    viewModel.signUpWithEmail(email, password) { success ->
                                        isLoading = false
                                        if (success) onNavigateToHome()
                                        else errorMessage = "Erro no cadastro"
                                    }
                                } else {
                                    viewModel.signInWithEmail(email, password) { success ->
                                        isLoading = false
                                        if (success) onNavigateToHome()
                                        else errorMessage = "Dados incorretos"
                                    }
                                }
                            } else {
                                errorMessage = "Verifique os campos"
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { isRegistering = !isRegistering },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isRegistering) "Já possui conta? Entre" else "Novo por aqui? Crie uma conta",
                            color = MedicleanTeal,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (isLoading && (showEmailLogin || showPhoneLogin)) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = MedicleanTeal, strokeWidth = 6.dp)
            }
        }
    }
}

@Composable
fun AuthSection(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanTeal)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MedicleanDarkGreen)
        }
        Spacer(Modifier.height(32.dp))
        content()
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        leadingIcon = { Icon(icon, contentDescription = null, tint = MedicleanTeal) },
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MedicleanDarkGreen,
            unfocusedTextColor = MedicleanDarkGreen,
            focusedBorderColor = MedicleanTeal,
            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.2f),
            focusedContainerColor = if(false) Color.White.copy(alpha = 0.05f) else Color.White,
            unfocusedContainerColor = if(false) Color.White.copy(alpha = 0.05f) else Color.White,
            focusedLabelColor = MedicleanTeal,
            unfocusedLabelColor = MedicleanDarkGreen.copy(alpha = 0.4f)
        )
    )
}

@Composable
fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
        enabled = !isLoading,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        } else {
            Text(text, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
        }
    }
}
