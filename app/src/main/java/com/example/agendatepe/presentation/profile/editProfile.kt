package com.example.agendatepe.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

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
                .addOnFailureListener {
                    name = user.displayName ?: ""
                    isLoading = false
                }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Azul)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navigateToHome() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Spacer(modifier = Modifier.weight(1f))
            Text("Mi Perfil", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { auth.signOut(); navigateToLogin() }) { Icon(Icons.Default.Logout, null, tint = Color.Red) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Crema)
        } else {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White).border(2.dp, Crema, CircleShape)
                    .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else if (imageUrl.isNotEmpty()) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.Person, null, tint = Azul, modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Toca para cambiar foto", color = Crema, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Text("Nombre Completo", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = black, unfocusedTextColor = black)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Teléfono (9 dígitos)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            // --- CAMPO DE TELÉFONO CON LÓGICA ---
            TextField(
                value = phone,
                onValueChange = { input ->
                    // 1. Solo permitir números y máximo 9 dígitos
                    if (input.all { it.isDigit() } && input.length <= 9) {
                        phone = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Teclado numérico
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedTextColor = black, unfocusedTextColor = black
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isGoogleUser) {
                Divider(color = Color.Gray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Seguridad", color = Crema, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { showChangeEmailDialog = true }, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) { Icon(Icons.Default.Email, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text(email.ifEmpty { "Correo" }, color = Color.White); Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.Edit, null, tint = Crema) }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { if(email.isNotEmpty()) auth.sendPasswordResetEmail(email).addOnSuccessListener { Toast.makeText(context, "Correo enviado", Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) { Icon(Icons.Default.Lock, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Cambiar Contraseña", color = Color.White); Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowForward, null, tint = Crema) }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    // 2. VALIDACIÓN AL GUARDAR
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Ingresa tu nombre", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (phone.length != 9) {
                        Toast.makeText(context, "El teléfono debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    val uid = auth.currentUser?.uid ?: return@Button

                    fun updateData(url: String) {
                        val map = hashMapOf("id" to uid, "email" to email, "name" to name, "phone" to phone, "image" to url)
                        FirebaseFirestore.getInstance().collection("users").document(uid).set(map, SetOptions.merge())
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Perfil Guardado", Toast.LENGTH_SHORT).show()
                                navigateToHome()
                            }
                    }

                    if (imageUri != null) {
                        val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                        ref.putFile(imageUri!!).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { url -> updateData(url.toString()) } }
                    } else {
                        updateData(imageUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Crema)
            ) {
                Text("Guardar y Continuar", color = black, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showChangeEmailDialog) {
        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            containerColor = Color.White,
            title = { Text("Cambiar Correo", color = black, fontWeight = FontWeight.Bold) },
            text = {
                var passwordVerify by remember { mutableStateOf("") }
                var newEmail by remember { mutableStateOf("") }
                Column {
                    Text("Ingresa tu contraseña actual y el nuevo correo.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = passwordVerify, onValueChange = { passwordVerify = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = black, unfocusedTextColor = black))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Nuevo Correo") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = black, unfocusedTextColor = black))
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