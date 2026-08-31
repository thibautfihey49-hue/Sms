package com.thibautfihey.sms.ui
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.File

@Composable fun DialPadScreenGlass(nav: NavController){
    var number by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally){
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))) { 
            Box(Modifier.padding(20.dp)) { Text(number.ifEmpty{"Numéro"}, fontSize=32.sp, fontWeight=FontWeight.Light) } 
        }
        Spacer(Modifier.height(24.dp))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach{ row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly){
                row.forEach{ d -> ElevatedButton(onClick={number+=d}, shape=CircleShape, modifier=Modifier.size(84.dp), colors=ButtonDefaults.elevatedButtonColors(containerColor=Color.White.copy(0.8f))){ Text(d, fontSize=26.sp) } }
            }
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick={ if(number.isNotEmpty()) try{ nav.navigate("chat/$number") }catch(_:Exception){} }, Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF6C63FF))){ Text("Appeler • SMS") }
    }
}
data class ThreadPreview(val name:String, val last:String, val unread:Int)
@Composable fun MessagesListScreenGlass(nav: NavController){
    val list = listOf(ThreadPreview("Maman","Tu rentres?",2), ThreadPreview("+33 6 12...","🎙️ Note vocale 0:24",0), ThreadPreview("Léa","MMS photo.jpg",1))
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Messages", style=MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){ items(list){ t -> 
            Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))) {
                Column(Modifier.padding(16.dp)){
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween){ Column{ Text(t.name, style=MaterialTheme.typography.titleMedium); Text(t.last, color=Color.Gray) }; Badge{ Text(t.unread.toString()) } }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick={ try{ nav.navigate("chat/${t.name}") }catch(_:Exception){} }, shape=RoundedCornerShape(12.dp)){ Text("Ouvrir") }
                }
            }
        } }
    }
}
@Composable fun ChatScreenGlass(number:String){
    val ctx = LocalContext.current
    var text by remember{mutableStateOf("")}
    var rec by remember{mutableStateOf(false)}
    var recorder by remember{mutableStateOf<MediaRecorder?>(null)}
    var file by remember{mutableStateOf<File?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){ Box(Modifier.padding(16.dp)){ Text("Chat $number", style=MaterialTheme.typography.titleMedium) } }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){
            Column(Modifier.padding(16.dp)){
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween){ 
                    Text(if(rec)"● Enregistrement..." else "Note vocale")
                    Button(onClick={
                        try{
                            if(!rec){
                                val f=File(ctx.cacheDir,"voice_${System.currentTimeMillis()}.m4a")
                                file=f
                                recorder=MediaRecorder().apply{
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(f.absolutePath)
                                    prepare()
                                    start()
                                }
                                rec=true
                                error=null
                            } else {
                                try{ recorder?.stop() }catch(_:Exception){}
                                recorder?.release()
                                recorder=null
                                rec=false
                            }
                        }catch(e:Exception){ error=e.message; rec=false }
                    }, colors=ButtonDefaults.buttonColors(containerColor=if(rec) Color.Red else Color(0xFF6C63FF)), shape=RoundedCornerShape(12.dp)){ Text(if(rec)"Stop" else "🎙️") }
                }
                file?.let{ Text("Prêt: ${it.name}", style=MaterialTheme.typography.labelSmall, modifier=Modifier.padding(top=8.dp)) }
                error?.let{ Text("Erreur: $it", color=Color.Red, style=MaterialTheme.typography.labelSmall) }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=text, onValueChange={text=it}, placeholder={Text("SMS / MMS")}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp))
        Spacer(Modifier.height(8.dp))
        Button(onClick={}, Modifier.fillMaxWidth().height(50.dp), shape=RoundedCornerShape(16.dp)){ Text("Envoyer ${if(file!=null) "+ vocale" else ""}") }
    }
}
@Composable fun ContactsScreenGlass(nav: NavController){
    var q by remember{mutableStateOf("")}
    val contacts = listOf("Alice","Bob","Maman","Léa")
    Column(Modifier.fillMaxSize().padding(16.dp)){
        OutlinedTextField(q,{q=it}, placeholder={Text("Rechercher")}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){ items(contacts.filter{it.contains(q,ignoreCase=true)}){ c -> 
            Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){
                Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween){ Text(c); TextButton(onClick={ try{ nav.navigate("chat/$c") }catch(_:Exception){} }){Text("SMS")} }
            }
        } }
    }
}
@Composable fun HistoryScreenGlass(){
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Historique", style=MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(12.dp))
        listOf("Maman • 10:42 • 2m13" to "Entrant","06 12.. • Hier • 0m24" to "Manqué").forEach{ (i,t) -> 
            Card(Modifier.fillMaxWidth().padding(bottom=10.dp), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){
                Column(Modifier.padding(16.dp)){ Text(i); Text(t, style=MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
