package com.thibautfihey.sms
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.thibautfihey.sms.ui.*

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande permissions sans crasher
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }

        setContent {
            GlassSmsApp()
        }
    }
}

@Composable
fun GlassSmsApp() {
    val navController = rememberNavController()
    var tab by remember { mutableStateOf(1) }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6C63FF),
            background = Color.White,
            surface = Color.White.copy(0.6f)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEDE7FF), Color(0xFFD6EFFF), Color.White)
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(0.55f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Clavier" to 0, "SMS" to 1, "Contacts" to 2, "Historique" to 3).forEach { (label, idx) ->
                            FilterChip(
                                selected = tab == idx,
                                onClick = {
                                    tab = idx
                                    when (idx) {
                                        0 -> navController.navigate("dial") { popUpTo(0) { inclusive = false }; launchSingleTop = true }
                                        1 -> navController.navigate("messages") { popUpTo(0) { inclusive = false }; launchSingleTop = true }
                                        2 -> navController.navigate("contacts") { popUpTo(0) { inclusive = false }; launchSingleTop = true }
                                        3 -> navController.navigate("history") { popUpTo(0) { inclusive = false }; launchSingleTop = true }
                                    }
                                },
                                label = { Text(label) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6C63FF).copy(0.9f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            ) { pad ->
                NavHost(
                    navController = navController,
                    startDestination = "messages",
                    modifier = Modifier.padding(pad)
                ) {
                    composable("dial") { DialPadScreenGlass(navController) }
                    composable("messages") { MessagesListScreenGlass(navController) }
                    composable("contacts") { ContactsScreenGlass(navController) }
                    composable("history") { HistoryScreenGlass() }
                    composable("chat/{number}") { backStack ->
                        val num = backStack.arguments?.getString("number") ?: ""
                        ChatScreenGlass(num)
                    }
                }
            }
        }
    }
}
