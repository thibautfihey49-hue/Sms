package com.thibautfihey.sms
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.thibautfihey.sms.ui.*

class MainActivity : ComponentActivity() {
    private val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO)
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toReq = perms.filter { ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }
        if(toReq.isNotEmpty()) launcher.launch(toReq.toTypedArray())

        setContent {
            var isDefault by remember { mutableStateOf(Telephony.Sms.getDefaultSmsPackage(this) == packageName) }
            GlassThemeV3(isDefault = isDefault, onSetDefault = {
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply{
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }.also{ startActivity(it) }
            })
        }
    }
}

@Composable
fun GlassThemeV3(isDefault: Boolean, onSetDefault: ()->Unit){
    val nav = rememberNavController()
    var tab by remember { mutableStateOf(1) }
    MaterialTheme(colorScheme = lightColorScheme(primary=Color(0xFF6C63FF), background=Color.Transparent)){
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF5F0FF), Color(0xFFE0F0FF), Color(0xFFFFFFFF))))){
            // Blur background layer
            Box(Modifier.fillMaxSize().blur(60.dp).background(Brush.radialGradient(listOf(Color(0xFF6C63FF).copy(0.15f), Color.Transparent), radius=800f)))
            Scaffold(containerColor=Color.Transparent,
                topBar={
                    if(!isDefault){
                        Card(Modifier.fillMaxWidth().padding(16.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF6C63FF).copy(0.9f))){
                            Row(Modifier.padding(16.dp), horizontalArrangement=Arrangement.SpaceBetween){
                                Text("Mettre Glass SMS par défaut ?", color=Color.White, modifier=Modifier.weight(1f))
                                Button(onClick=onSetDefault, colors=ButtonDefaults.buttonColors(containerColor=Color.White)){ Text("Activer", color=Color(0xFF6C63FF)) }
                            }
                        }
                    }
                },
                bottomBar={
                    Row(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(0.45f)).padding(10.dp), horizontalArrangement=Arrangement.SpaceEvenly){
                        listOf("Clavier" to 0, "SMS" to 1, "Contacts" to 2, "Appels" to 3).forEach{ (l,i)->
                            FilterChip(selected=tab==i, onClick={tab=i; when(i){0->nav.navigate("dial"){launchSingleTop=true};1->nav.navigate("messages"){launchSingleTop=true};2->nav.navigate("contacts"){launchSingleTop=true};3->nav.navigate("history"){launchSingleTop=true}}}, label={Text(l)}, shape=RoundedCornerShape(50), colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Color(0xFF6C63FF), selectedLabelColor=Color.White))
                        }
                    }
                }
            ){pad->
                NavHost(nav, startDestination="messages", modifier=Modifier.padding(pad)){
                    composable("dial"){ DialPadScreenGlass(nav) }
                    composable("messages"){ MessagesListScreenGlass(nav) }
                    composable("contacts"){ ContactsScreenGlass(nav) }
                    composable("history"){ HistoryScreenGlass() }
                    composable("chat/{number}"){ b-> ChatScreenGlass(b.arguments?.getString("number")?:"") }
                }
            }
        }
    }
}
