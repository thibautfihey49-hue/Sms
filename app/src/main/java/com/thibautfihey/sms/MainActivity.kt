package com.thibautfihey.sms
import android.Manifest; import android.app.role.RoleManager; import android.content.Intent; import android.content.pm.PackageManager; import android.os.Build; import android.os.Bundle; import android.provider.Telephony
import androidx.activity.ComponentActivity; import androidx.activity.compose.setContent; import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.shape.RoundedCornerShape; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.clip; import androidx.compose.ui.graphics.Brush; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalLifecycleOwner; import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat; import androidx.lifecycle.Lifecycle; import androidx.lifecycle.LifecycleEventObserver; import androidx.navigation.compose.rememberNavController; import androidx.navigation.compose.NavHost; import androidx.navigation.compose.composable
import com.thibautfihey.sms.ui.*

class MainActivity : ComponentActivity() {
    private val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toReq = perms.filter { ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }
        if(toReq.isNotEmpty()) permLauncher.launch(toReq.toTypedArray())
        setContent { GlassAppV6(onSetDefault={
            try{
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    val rm = getSystemService(RoleManager::class.java)
                    if(rm.isRoleAvailable(RoleManager.ROLE_SMS)){ roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS)) }
                } else {
                    val i = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT); i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName); roleLauncher.launch(i)
                }
            }catch(_:Exception){ try{ startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }catch(_:Exception){} }
        }) }
    }
}

@Composable
fun GlassAppV6(onSetDefault:()->Unit){
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var isDefault by remember { mutableStateOf(Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName) }
    // Re-vérifie à chaque retour dans l'app -> fait disparaître la bannière
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner){
        val obs = LifecycleEventObserver{_,e-> if(e==Lifecycle.Event.ON_RESUME){ isDefault = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName } }
        lifecycleOwner.lifecycle.addObserver(obs); onDispose{ lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val nav = rememberNavController(); var tab by remember { mutableStateOf(1) }
    MaterialTheme(colorScheme = lightColorScheme(primary=Color(0xFF7C6CFF))){
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF3EFFF), Color(0xFFE8F3FF), Color.White)))){
            Scaffold(containerColor=Color.Transparent,
                topBar={ if(!isDefault){ Card(Modifier.fillMaxWidth().padding(16.dp), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF7C6CFF))){ Row(Modifier.padding(18.dp), horizontalArrangement=Arrangement.SpaceBetween){ Column(Modifier.weight(1f)){ Text("Glass SMS n'est pas par défaut", color=Color.White, style=MaterialTheme.typography.titleSmall); Text("Active pour recevoir les SMS", color=Color.White.copy(0.8f), style=MaterialTheme.typography.labelSmall) }; Button(onClick=onSetDefault, colors=ButtonDefaults.buttonColors(containerColor=Color.White), shape=RoundedCornerShape(50)){ Text("Activer", color=Color(0xFF7C6CFF)) } } } } },
                bottomBar={
                    Row(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(0.8f)).padding(6.dp), horizontalArrangement=Arrangement.SpaceEvenly){
                        listOf("Clavier" to 0, "SMS" to 1, "Contacts" to 2).forEach{ (l,i)-> FilterChip(selected=tab==i, onClick={tab=i; when(i){0->nav.navigate("dial"){launchSingleTop=true};1->nav.navigate("messages"){launchSingleTop=true};2->nav.navigate("contacts"){launchSingleTop=true}}}, label={Text(l)}, shape=RoundedCornerShape(50), colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Color(0xFF7C6CFF), selectedLabelColor=Color.White)) }
                    }
                }
            ){pad-> NavHost(nav, startDestination="messages", modifier=Modifier.padding(pad)){ composable("dial"){ DialPadScreenGlassV2(nav) }; composable("messages"){ MessagesListScreenGlassV4(nav) }; composable("contacts"){ ContactsScreenGlass(nav) }; composable("chat/{number}"){ b-> ChatScreenGlass(b.arguments?.getString("number")?:"") } } }
        }
    }
}
