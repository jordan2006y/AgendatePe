package com.example.agendatepe.presentation.initial

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.Azul
import com.example.agendatepe.ui.theme.Crema
import com.example.agendatepe.ui.theme.shapeButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

@Composable
fun InitialScreen(
    navigateToLogin: () -> Unit,
    navigateToSignUp: () -> Unit,
    navigateToHome: () -> Unit,
    navigateToProfileSetup: () -> Unit // <--- AHORA APUNTA A 'ProfileScreen'
) {
    val context = LocalContext.current
    val auth = Firebase.auth

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("966542507563-u887qkq92gh3oeda403jc4a93odffugd.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val db = FirebaseFirestore.getInstance()
                            val docRef = db.collection("users").document(user.uid)

                            docRef.get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // USUARIO ANTIGUO -> AL HOME
                                    Log.i("aris", "Usuario existe, al Home")
                                    navigateToHome()
                                } else {
                                    // USUARIO NUEVO -> A CONFIGURAR PERFIL (ProfileScreen)
                                    Log.i("aris", "Usuario nuevo, configurando perfil")
                                    navigateToProfileSetup()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Error Auth", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Log.e("aris", "Error Google: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(color = Azul),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(id = R.drawable.ic_logo_inicio), contentDescription = "Logo", modifier = Modifier.size(200.dp).padding(bottom = 32.dp))
        Image(painter = painterResource(id = R.drawable.ic_inicio), contentDescription = "Inicio", modifier = Modifier.size(300.dp))
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navigateToLogin() },
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crema)
        ) { Text("Iniciar Sesión", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navigateToSignUp() },
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crema)
        ) { Text("Registrarse", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))

        CustomButton(
            modifier = Modifier.clickable { launcher.launch(googleSignInClient.signInIntent) },
            painter = painterResource(id = R.drawable.google),
            title = "Continuar con Google"
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomButton(modifier: Modifier, painter: Painter, title: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 32.dp).background(color = Crema, shape = CircleShape).border(2.dp, shapeButton, CircleShape).then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(painter = painter, contentDescription = null, modifier = Modifier.padding(start = 16.dp).size(24.dp))
        Text(text = title, color = Color.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}