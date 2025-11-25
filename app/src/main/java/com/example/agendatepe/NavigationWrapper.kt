package com.example.agendatepe

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.agendatepe.presentation.initial.InitialScreen
import com.example.agendatepe.presentation.login.LoginScreen
import com.example.agendatepe.presentation.profile.EditProfileScreen
import com.example.agendatepe.presentation.profile.ProfileScreen
import com.example.agendatepe.presentation.signup.SingUpScreen
import com.example.agendatepe.presentation.home.HomeScreen
import com.example.agendatepe.presentation.home.LocationPickerScreen
import com.example.agendatepe.presentation.home.PublishPropertyScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    startDestination: String
) {
    val context = LocalContext.current

    NavHost(navController = navHostController, startDestination = startDestination) {

        composable("initialScreen") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate("logIn") },
                navigateToSignUp = { navHostController.navigate("SignUp") },
                navigateToHome = {
                    navHostController.navigate("home") { popUpTo("initialScreen") { inclusive = true } }
                },
                navigateToProfileSetup = {
                    val email = auth.currentUser?.email ?: "user"
                    navHostController.navigate("profile/$email/GOOGLE_LOGIN")
                }
            )
        }

        composable("logIn"){
            LoginScreen(
                auth,
                navigateToHome = {
                    navHostController.navigate("home") { popUpTo("initialScreen") { inclusive = true } }
                },
                onBack = { navHostController.popBackStack() }
            )
        }

        composable("SignUp"){
            SingUpScreen(
                navigateToProfile = { email, password ->
                    navHostController.navigate("profile/$email/$password")
                },
                onBack = { navHostController.popBackStack() }
            )
        }

        composable(
            route = "profile/{email}/{password}",
            arguments = listOf(navArgument("email") { type = NavType.StringType }, navArgument("password") { type = NavType.StringType })
        ) { entry ->
            ProfileScreen(
                auth = auth,
                emailRecibido = entry.arguments?.getString("email") ?: "",
                passwordRecibido = entry.arguments?.getString("password") ?: "",
                navigateToHome = {
                    navHostController.navigate("home") { popUpTo("initialScreen") { inclusive = true } }
                }
            )
        }

        composable("home") {
            HomeScreen(
                navigateToProfile = { navHostController.navigate("edit_profile") },
                onLogout = {
                    // --- CORRECCIÓN: CERRAR SESIÓN DE GOOGLE + FIREBASE ---
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("966542507563-u887qkq92gh3oeda403jc4a93odffugd.apps.googleusercontent.com")
                        .requestEmail()
                        .build()

                    val googleSignInClient = GoogleSignIn.getClient(context, gso)

                    // 1. Cerramos sesión en el cliente de Google para olvidar la cuenta
                    googleSignInClient.signOut().addOnCompleteListener {
                        // 2. Cerramos sesión en Firebase
                        auth.signOut()
                        // 3. Volvemos al inicio
                        navHostController.navigate("initialScreen") { popUpTo(0) { inclusive = true } }
                    }
                },
                navigateToPublish = { tipo -> navHostController.navigate("publish/$tipo") }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(auth, navigateToHome = { navHostController.popBackStack() }, navigateToLogin = { navHostController.navigate("initialScreen") { popUpTo(0) { inclusive = true } } })
        }

        composable("publish/{tipo}", arguments = listOf(navArgument("tipo") { type = NavType.StringType })) { entry ->
            PublishPropertyScreen(auth, entry.arguments?.getString("tipo") ?: "Venta", navController = navHostController)
        }

        composable("pick_location") {
            LocationPickerScreen(
                onLocationSelected = { lat, lng, address ->
                    navHostController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("location_lat", lat); set("location_lng", lng); set("location_address", address)
                    }
                    navHostController.popBackStack()
                },
                onDismiss = { navHostController.popBackStack() }
            )
        }
    }
}