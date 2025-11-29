package com.example.agendatepe.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agendatepe.presentation.home.glassEffect
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    auth: FirebaseAuth,
    navigateToHome: () -> Unit,
    navigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // --- PROTECCIÓN SALIDA ---
    BackHandler { showExitDialog = true }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir sin guardar?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Se perderán los cambios.", color = Color.Gray) },
            confirmButton = { Button(onClick = { showExitDialog = false; navigateToHome() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurface) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    var isGoogleUser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            email = user.email ?: ""
            isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        name = doc.getString("name") ?: ""
                        phone = doc.getString("phone") ?: ""
                        imageUrl = doc.getString("image") ?: ""
                    } else {
                        name = user.displayName ?: ""
                        imageUrl = user.photoUrl?.toString() ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener { name = user.displayName ?: ""; isLoading = false }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { imageUri = it }

    // --- UI PREMIUM ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Negro
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { showExitDialog = true }) { Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground) } },
                actions = { IconButton(onClick = { auth.signOut(); navigateToLogin() }) { Icon(Icons.Default.Logout, null, tint = Color.Red) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // FOTO DE PERFIL (HERO)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.BottomEnd
            ) {
                // Imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, Azul, CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (imageUrl.isNotEmpty()) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                }

                // Icono Cámara (Glass Effect)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .glassEffect(backgroundColor = Azul, shape = CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape), // Borde negro para separar
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Cambiar Foto", color = Azul, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            // FORMULARIO
            Text("Información Personal", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Azul) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Azul, unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Azul, unfocusedLabelColor = Color.Gray,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 9) phone = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = Azul) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Azul, unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Azul, unfocusedLabelColor = Color.Gray,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SECCIÓN SEGURIDAD
            if (!isGoogleUser) {
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Seguridad", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))

                // Botón Cambiar Correo
                OutlinedButton(
                    onClick = { showChangeEmailDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.Email, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(email.ifEmpty { "Correo" }, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, tint = Azul)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Cambiar Contraseña
                OutlinedButton(
                    onClick = { if(email.isNotEmpty()) auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context, "Correo enviado", Toast.LENGTH_LONG).show() } },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Restablecer Contraseña", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, tint = Azul)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN GUARDAR (Con Sombra Azul)
            Button(
                onClick = {
                    if (name.isEmpty() || phone.length != 9) {
                        Toast.makeText(context, "Verifica tu nombre y teléfono (9 dígitos)", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        val uid = auth.currentUser?.uid ?: return@Button
                        fun uploadData(url: String) {
                            val map = hashMapOf("id" to uid, "email" to email, "name" to name, "phone" to phone, "image" to url)
                            FirebaseFirestore.getInstance().collection("users").document(uid).set(map, SetOptions.merge())
                                .addOnSuccessListener { isLoading = false; navigateToHome() }
                        }
                        if (imageUri != null) {
                            val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                            ref.putFile(imageUri!!).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { uploadData(it.toString()) } }
                        } else uploadData(imageUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Azul),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azul)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showChangeEmailDialog) {
        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Cambiar Correo", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                var passwordVerify by remember { mutableStateOf("") }
                var newEmail by remember { mutableStateOf("") }
                Column {
                    Text("Confirma tu identidad para continuar.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = passwordVerify, onValueChange = { passwordVerify = it }, label = { Text("Contraseña Actual") }, visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Nuevo Correo") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val user = auth.currentUser
                            if (user != null && user.email != null && passwordVerify.isNotEmpty() && newEmail.isNotEmpty()) {
                                val credential = EmailAuthProvider.getCredential(user.email!!, passwordVerify)
                                user.reauthenticate(credential).addOnSuccessListener {
                                    user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener {
                                        Toast.makeText(context, "Verificación enviada", Toast.LENGTH_LONG).show()
                                        showChangeEmailDialog = false
                                    }
                                }.addOnFailureListener { Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)
                    ) { Text("Verificar y Cambiar", color = Color.White) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showChangeEmailDialog = false }) { Text("Cancelar", color = Color.Red) } }
        )
    }
}