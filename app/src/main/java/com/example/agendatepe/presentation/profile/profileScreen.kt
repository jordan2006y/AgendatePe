package com.example.agendatepe.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    emailRecibido: String,
    passwordRecibido: String,
    navigateToHome: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // --- PROTECCIÓN DE SALIDA ---
    var showExitDialog by remember { mutableStateOf(false) }
    // Como esta es una pantalla "intermedia" de registro, el "atrás" podría ser cancelar registro.
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cancelar registro?") },
            text = { Text("Si sales ahora, no se completará tu perfil.") },
            confirmButton = { Button(onClick = { showExitDialog = false; /* No hay navegación atrás clara aquí, pero podríamos cerrar app o ir a login */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continuar Editando", color = black) } },
            containerColor = Color.White
        )
    }
    // ----------------------------

    val googleUser = auth.currentUser
    val isGoogleLogin = passwordRecibido == "GOOGLE_LOGIN" || (googleUser != null && passwordRecibido.isEmpty())

    LaunchedEffect(Unit) {
        if (isGoogleLogin && googleUser != null) {
            name = googleUser.displayName ?: ""
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Azul)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "AgendatePé", color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Configuración de Perfil",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Crema)
                .clickable {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = black, modifier = Modifier.size(50.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Toca para subir foto", color = Color.White, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Nombre Completo", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = name, onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Crema, unfocusedContainerColor = Crema,
                focusedTextColor = black, unfocusedTextColor = black,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Número de Telefono", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = phone,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 9) {
                    phone = input
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Crema,
                unfocusedContainerColor = Crema,
                focusedTextColor = black,
                unfocusedTextColor = black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator(color = Crema)
        } else {
            Button(
                onClick = {
                    if (name.isNotEmpty() && phone.length == 9) {
                        isLoading = true

                        fun saveFirestoreData(uid: String) {
                            val saveData = { url: String ->
                                val map = hashMapOf(
                                    "id" to uid,
                                    "name" to name,
                                    "phone" to phone,
                                    "email" to (auth.currentUser?.email ?: emailRecibido),
                                    "image" to url
                                )
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .set(map, SetOptions.merge())
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navigateToHome()
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            if (imageUri != null) {
                                val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                                ref.putFile(imageUri!!).addOnSuccessListener {
                                    ref.downloadUrl.addOnSuccessListener { url -> saveData(url.toString()) }
                                }
                            } else {
                                val currentPhoto = auth.currentUser?.photoUrl?.toString() ?: ""
                                saveData(currentPhoto)
                            }
                        }

                        if (isGoogleLogin) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) saveFirestoreData(uid)
                        } else {
                            auth.createUserWithEmailAndPassword(emailRecibido, passwordRecibido)
                                .addOnSuccessListener { res ->
                                    val uid = res.user!!.uid
                                    saveFirestoreData(uid)
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Error al crear cuenta", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(context, "Completa el nombre y un teléfono de 9 dígitos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Crema)
            ) {
                Text(text = "Guardar y Continuar", color = black, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}