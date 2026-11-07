
@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    var user by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser)
    }

    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val authManager = remember(activity) {
        activity?.let { GoogleAuthManager(it) }
    }

    val signInLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            loading = false

            try {
                val manager = authManager

                if (manager == null) {
                    message = "Google Sign-In is unavailable."
                } else {
                    manager.handleSignInResult(result.data)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val signedInUser =
                                    FirebaseAuth.getInstance().currentUser

                                if (signedInUser != null) {

                                    UserProfileRepository.saveUserProfile(
                                        user = signedInUser,

                                        onSuccess = {
                                            user = FirebaseAuth.getInstance().currentUser
                                            message = "Google sign-in successful. Profile saved."
                                        },

                                        onError = { error ->
                                            user = FirebaseAuth.getInstance().currentUser
                                            message = "Profile saving failed: $error"
                                        }
                                    )

                                } else {
                                    message = "User details unavailable."
                                }

                            } else {
                                message = "Authentication failed: ${
                                    task.exception?.localizedMessage
                                        ?: "Unknown error"
                                }"
                            }
                        }
                }

            } catch (e: Exception) {
                message = "Google Sign-In error: ${
                    e.localizedMessage ?: e.javaClass.simpleName
                }"
            }
        }

    Column(Modifier.fillMaxSize()) {

        PurpleHeader(
            "My Account",
            "Your Caller Pro profile"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                Icons.Default.AccountCircle,
                null,
                tint = Purple,
                modifier = Modifier.size(76.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Caller Pro Account",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = AppText
            )

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(Modifier.padding(20.dp)) {

                    if (user != null) {

                        Text(
                            "Signed in",
                            color = Purple,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            user?.displayName ?: "Google user",
                            color = AppText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            user?.email ?: "",
                            color = MutedText,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        OutlinedButton(
                            onClick = {
                                loading = true

                                authManager?.signOut {
                                    user = null
                                    loading = false
                                    message = "You have signed out."
                                } ?: run {
                                    loading = false
                                    message = "Unable to sign out."
                                }
                            },
                            enabled = !loading && authManager != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out")
                        }

                    } else {

                        Text(
                            "Google sign-in is optional.",
                            color = AppText,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "You can use available local features without an account. Sign in to access family groups and cloud backup.",
                            color = MutedText,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (authManager == null) {
                                    message = "Unable to open Google sign-in."
                                } else {
                                    loading = true

                                    signInLauncher.launch(
                                        authManager.getSignInIntent()
                                    )
                                }
                            },
                            enabled = !loading && authManager != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {

                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(19.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )

                                Spacer(Modifier.width(8.dp))
                            }

                            Text(
                                if (loading) "Please wait..."
                                else "Sign in with Google"
                            )
                        }
                    }

                    message?.let {

                        Spacer(Modifier.height(12.dp))

                        Text(
                            it,
                            color = Purple,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            AccountOption(
                Icons.Default.Security,
                "Privacy & Security"
            )

            AccountOption(
                Icons.Default.Cloud,
                "Backup & Restore"
            )

            AccountOption(
                Icons.Default.Info,
                "About Caller Pro"
            )
        }
    }
}
