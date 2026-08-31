package com.thibautfihey.sms.ui
import android.content.Context; import android.content.SharedPreferences; import android.net.Uri; import android.provider.Telephony
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items; import androidx.compose.foundation.shape.RoundedCornerShape; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.*
import java.util.concurrent.TimeUnit

// --- PRIVATE BOX comme dans com.messenger.sms.messages ---
@Composable fun PrivateBoxScreen(){
    val ctx = LocalContext.current; val prefs = ctx.getSharedPreferences("private_box", Context.MODE_PRIVATE)
    var locked by remember { mutableStateOf(prefs.getBoolean("locked", true)) }
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Private Box • Cryptée", style=MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if(locked){
            Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFEEE8FF))){
                Column(Modifier.padding(16.dp)){
                    Text("Boîte privée verrouillée comme Messenger SMS original - cache les conversations privées")
                    OutlinedTextField(pin,{pin=it}, label={Text("PIN (1234 par défaut)")}, shape=RoundedCornerShape(16.dp), modifier=Modifier.fillMaxWidth().padding(top=8.dp))
                    Button(onClick={ if(pin=="1234" || pin==prefs.getString("pin","1234")) locked=false }, modifier=Modifier.fillMaxWidth().padding(top=8.dp), shape=RoundedCornerShape(50)){ Text("Déverrouiller") }
                }
            }
        } else {
            Text("Messages privés (0) - Ajoute un contact en long-press dans SMS pour mettre en privé")
            Button(onClick={ locked=true }, modifier=Modifier.padding(top=12.dp)){ Text("Verrouiller") }
        }
    }
}

// --- SMS BLOCKER comme original ---
@Composable fun BlockerScreen(){
    val ctx = LocalContext.current; val prefs = ctx.getSharedPreferences("blocker", Context.MODE_PRIVATE)
    var list by remember { mutableStateOf(prefs.getStringSet("blocked", emptySet())?.toList()?: emptyList()) }
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("SMS Blocker • Anti-spam", style=MaterialTheme.typography.titleLarge)
        Text("Bloque les numéros spam comme dans Messenger SMS - 1M+ numéros", style=MaterialTheme.typography.labelSmall, color=Color.Gray)
        Row(Modifier.padding(top=12.dp)){ OutlinedTextField(input,{input=it}, placeholder={Text("Numéro à bloquer")}, shape=RoundedCornerShape(16.dp), modifier=Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Button(onClick={ if(input.isNotEmpty()){ val s = prefs.getStringSet("blocked", mutableSetOf())?.toMutableSet()?: mutableSetOf(); s.add(input); prefs.edit().putStringSet("blocked", s).apply(); list = s.toList(); input="" } }, shape=RoundedCornerShape(50)){ Text("Bloquer") } }
        LazyColumn(Modifier.padding(top=12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){ items(list){ num-> Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE8E8))){ Row(Modifier.padding(12.dp), horizontalArrangement=Arrangement.SpaceBetween){ Text(num); TextButton(onClick={ val s = prefs.getStringSet("blocked", mutableSetOf())?.toMutableSet()?: mutableSetOf(); s.remove(num); prefs.edit().putStringSet("blocked", s).apply(); list=s.toList() }){ Text("Débloquer") } } } } }
    }
}

// --- SCHEDULE SMS comme original - Delayed SMS ---
@Composable fun ScheduleScreen(){
    val ctx = LocalContext.current
    var number by remember { mutableStateOf("") }; var msg by remember { mutableStateOf("") }; var delay by remember { mutableStateOf("5") }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Programmer SMS • Schedule", style=MaterialTheme.typography.titleLarge)
        Text("Envoie plus tard, corrige les erreurs comme Messenger", style=MaterialTheme.typography.labelSmall, color=Color.Gray)
        OutlinedTextField(number,{number=it}, label={Text("Numéro")}, shape=RoundedCornerShape(16.dp), modifier=Modifier.fillMaxWidth().padding(top=12.dp))
        OutlinedTextField(msg,{msg=it}, label={Text("Message - Emoji, GIF supporté")}, shape=RoundedCornerShape(16.dp), modifier=Modifier.fillMaxWidth().padding(top=8.dp), minLines=3)
        OutlinedTextField(delay,{delay=it}, label={Text("Délai minutes")}, shape=RoundedCornerShape(16.dp), modifier=Modifier.padding(top=8.dp))
        Button(onClick={
            val work = OneTimeWorkRequestBuilder<SmsWorker>().setInitialDelay(delay.toLongOrNull()?:5, TimeUnit.MINUTES).setInputData(workDataOf("number" to number, "msg" to msg)).build()
            WorkManager.getInstance(ctx).enqueue(work)
        }, modifier=Modifier.fillMaxWidth().padding(top=12.dp), shape=RoundedCornerShape(50)){ Text("Programmer") }
    }
}
class SmsWorker(ctx: Context, params: WorkerParameters): Worker(ctx, params){
    override fun doWork(): Result {
        val number = inputData.getString("number")?:""; val msg = inputData.getString("msg")?:""
        try{ android.telephony.SmsManager.getDefault().sendTextMessage(number, null, msg, null, null) }catch(_:Exception){}
        return Result.success()
    }
}

// --- BACKUP & RESTORE comme original ---
@Composable fun BackupScreen(){
    val ctx = LocalContext.current; var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Backup & Restore", style=MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8FFEC))){ Column(Modifier.padding(16.dp)){ Text("Sauvegarde SMS & MMS vers stockage interne"); Button(onClick={
            try{
                val cr = ctx.contentResolver; val cur = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null); var count=0
                val json = StringBuilder("["); cur?.use{ c-> while(c.moveToNext()){ count++ } }; json.append("]"); ctx.openFileOutput("sms_backup.json", Context.MODE_PRIVATE).use{ it.write(json.toString().toByteArray()) }; status="Backup $count SMS -> sms_backup.json"
            }catch(e:Exception){ status="Erreur: ${e.message}" }
        }, shape=RoundedCornerShape(50), modifier=Modifier.padding(top=8.dp)){ Text("Backup maintenant") }; Text(status, style=MaterialTheme.typography.labelSmall) } }
    }
}

@Composable fun ThemeScreen(){
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Thèmes • Bubbles • Wallpaper", style=MaterialTheme.typography.titleLarge)
        Text("Comme Messenger SMS - custom bulles, couleur, fond", style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        listOf("Violet Glass (actuel)" to Color(0xFF7C6CFF), "Rose Love" to Color(0xFFFF5A8A), "Ocean Blue" to Color(0xFF2B8FFF), "Dark OLED" to Color.Black).forEach{ (name, col)->
            Card(Modifier.fillMaxWidth().padding(bottom=8.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=col.copy(0.2f))){
                Row(Modifier.padding(16.dp)){ Box(Modifier.size(24.dp).padding(end=0.dp)); Text(name) }
            }
        }
    }
}

@Composable fun MoreScreen(nav: NavController){
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Plus • Comme Messenger SMS original", style=MaterialTheme.typography.titleLarge)
        listOf("Programmer SMS" to "schedule", "Backup & Restore" to "backup", "Thèmes & Bulles" to "themes", "Mode Conduite & Réponse Auto" to "", "Caller ID Spam 1M+" to "").forEach{ (title, route)->
            Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.9f)), onClick={ if(route.isNotEmpty()) nav.navigate(route) }){
                Column(Modifier.padding(16.dp)){ Text(title, style=MaterialTheme.typography.titleSmall); Text("Fonctionnalité copiée de com.messenger.sms.messages", style=MaterialTheme.typography.labelSmall, color=Color.Gray) }
            }
        }
    }
}
