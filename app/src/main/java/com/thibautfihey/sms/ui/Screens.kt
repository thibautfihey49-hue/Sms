package com.thibautfihey.sms.ui

import android.content.ContentResolver
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
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

// === VRAIS SMS ===
data class ThreadPreview(val threadId: Long, val name: String, val last: String, val unread: Int, val number: String)

@Composable
fun MessagesListScreenGlass(nav: NavController){
    val ctx = LocalContext.current
    var threads by remember { mutableStateOf<List<ThreadPreview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit){
        try {
            val cr = ctx.contentResolver
            val uri = Uri.parse("content://mms-sms/conversations?simple=true")
            val list = mutableListOf<ThreadPreview>()
            val cursor: Cursor? = cr.query(uri, null, null, null, "date DESC")
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(Telephony.Threads._ID)
                val snippetIdx = c.getColumnIndex(Telephony.Threads.SNIPPET)
                val recipientIdsIdx = c.getColumnIndex(Telephony.Threads.RECIPIENT_IDS)
                while(c.moveToNext() && list.size < 100){
                    val threadId = if(idIdx>=0) c.getLong(idIdx) else 0L
                    val last = if(snippetIdx>=0) c.getString(snippetIdx) ?: "" else ""
                    // Récupère le numéro via canonical_addresses
                    var number = "Inconnu"
                    var name = "Inconnu"
                    if(recipientIdsIdx>=0){
                        val recId = c.getString(recipientIdsIdx)
                        try{
                            val addrCursor = cr.query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = ?", arrayOf(recId), null)
                            addrCursor?.use { ac ->
                                if(ac.moveToFirst()){
                                    number = ac.getString(0) ?: number
                                }
                            }
                        }catch(_:Exception){}
                    }
                    // Essaie de trouver le nom dans les contacts
                    try{
                        val contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                        val contactCur = cr.query(contactUri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                        contactCur?.use { cc ->
                            if(cc.moveToFirst()) name = cc.getString(0) ?: number else name = number
                        }
                    }catch(_:Exception){ name = number }

                    list.add(ThreadPreview(threadId, name, last, 0, number))
                }
            }
            threads = list
        }catch(e:Exception){
            threads = listOf(ThreadPreview(0,"Erreur SMS", e.message ?: "Autorise READ_SMS",0,""))
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Messages - VRAIS SMS", style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold)
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(8.dp))
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){
            items(threads){ t ->
                Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))) {
                    Column(Modifier.padding(16.dp)){
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween){
                            Column(Modifier.weight(1f)){
                                Text(t.name, style=MaterialTheme.typography.titleMedium, maxLines=1)
                                Text(t.number, style=MaterialTheme.typography.labelSmall, color=Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                Text(t.last, color=Color.Gray, maxLines=2)
                            }
                            if(t.unread>0) Badge{ Text(t.unread.toString()) }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick={ try{ nav.navigate("chat/${Uri.encode(t.number)}") }catch(_:Exception){} }, shape=RoundedCornerShape(12.dp)){ Text("Ouvrir") }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreenGlass(encodedNumber:String){
    val number = Uri.decode(encodedNumber)
    val ctx = LocalContext.current
    var messages by remember { mutableStateOf<List<Pair<String,Boolean>>>(emptyList()) }
    var text by remember{mutableStateOf("")}
    var rec by remember{mutableStateOf(false)}
    var recorder by remember{mutableStateOf<MediaRecorder?>(null)}
    var file by remember{mutableStateOf<File?>(null)}

    LaunchedEffect(number){
        try{
            val cr = ctx.contentResolver
            val cur = cr.query(Uri.parse("content://sms/"), null, "address = ?", arrayOf(number), "date ASC")
            val msgs = mutableListOf<Pair<String,Boolean>>()
            cur?.use { c ->
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)
                while(c.moveToNext()){
                    val body = if(bodyIdx>=0) c.getString(bodyIdx) ?: "" else ""
                    val type = if(typeIdx>=0) c.getInt(typeIdx) else 1
                    msgs.add(body to (type == 2)) // 2 = sent
                }
            }
            messages = msgs.takeLast(50)
        }catch(_:Exception){}
    }

    Column(Modifier.fillMaxSize().padding(16.dp)){
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){ Box(Modifier.padding(16.dp)){ Text("Chat $number", style=MaterialTheme.typography.titleMedium) } }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(messages){ (body, isMe) ->
                Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=if(isMe) Color(0xFF6C63FF).copy(0.9f) else Color.White.copy(0.8f)), modifier=Modifier.fillMaxWidth(0.85f).align(if(isMe) Alignment.End else Alignment.Start)){
                    Box(Modifier.padding(12.dp)){ Text(body, color=if(isMe) Color.White else Color.Black) }
                }
            }
        }
        // Zone envoi
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(value=text, onValueChange={text=it}, placeholder={Text("SMS")}, modifier=Modifier.weight(1f), shape=RoundedCornerShape(20.dp))
            Spacer(Modifier.width(8.dp))
            Button(onClick={
                try{
                    // Envoi SMS réel
                    val smsManager = android.telephony.SmsManager.getDefault()
                    smsManager.sendTextMessage(number, null, text, null, null)
                    messages = messages + (text to true)
                    text = ""
                }catch(_:Exception){}
            }, shape=CircleShape, modifier=Modifier.size(56.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF6C63FF))){ Text("➤") }
        }
    }
}

data class ContactReal(val name:String, val number:String)

@Composable
fun ContactsScreenGlass(nav: NavController){
    val ctx = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactReal>>(emptyList()) }
    var q by remember{mutableStateOf("")}

    LaunchedEffect(Unit){
        try{
            val cr = ctx.contentResolver
            val cur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")
            val list = mutableListOf<ContactReal>()
            cur?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while(c.moveToNext() && list.size < 500){
                    val n = if(nameIdx>=0) c.getString(nameIdx) ?: "" else ""
                    val num = if(numIdx>=0) c.getString(numIdx) ?: "" else ""
                    if(n.isNotEmpty() && num.isNotEmpty()) list.add(ContactReal(n,num))
                }
            }
            contacts = list.distinctBy { it.number }
        }catch(_:Exception){}
    }

    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Contacts - ${contacts.size} vrais", style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(q,{q=it}, placeholder={Text("Rechercher contact réel")}, modifier=Modifier.fillMaxWidth().padding(top=8.dp), shape=RoundedCornerShape(20.dp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(contacts.filter{it.name.contains(q,ignoreCase=true) || it.number.contains(q)}){ c ->
                Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                        Column{ Text(c.name, fontWeight=FontWeight.Medium); Text(c.number, style=MaterialTheme.typography.labelSmall, color=Color.Gray) }
                        Row{
                            TextButton(onClick={ try{ nav.navigate("chat/${Uri.encode(c.number)}") }catch(_:Exception){} }){Text("SMS")}
                            TextButton(onClick={
                                try{
                                    val intent = android.content.Intent(android.content.Intent.ACTION_CALL, Uri.parse("tel:${c.number}"))
                                    ctx.startActivity(intent)
                                }catch(_:Exception){}
                            }){Text("Appel")}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialPadScreenGlass(nav: NavController){
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
        Button(onClick={ if(number.isNotEmpty()) try{ nav.navigate("chat/${Uri.encode(number)}") }catch(_:Exception){} }, Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF6C63FF))){ Text("SMS / Appeler") }
    }
}

@Composable
fun HistoryScreenGlass(){
    val ctx = LocalContext.current
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit){
        try{
            val cr = ctx.contentResolver
            val cur = cr.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE), null, null, CallLog.Calls.DATE+" DESC LIMIT 100")
            val list = mutableListOf<String>()
            cur?.use { c ->
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val durIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                while(c.moveToNext()){
                    val name = if(nameIdx>=0) c.getString(nameIdx) ?: "" else ""
                    val num = if(numIdx>=0) c.getString(numIdx) ?: "" else ""
                    val dur = if(durIdx>=0) c.getString(durIdx) ?: "0" else "0"
                    val type = if(typeIdx>=0) c.getInt(typeIdx) else 0
                    val t = when(type){ 1->"Entrant"; 2->"Sortant"; 3->"Manqué"; else->"Autre" }
                    list.add("${if(name.isNotEmpty()) name else num} • $t • ${dur}s")
                }
            }
            logs = list
        }catch(_:Exception){ logs = listOf("Autorise READ_CALL_LOG") }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Historique réel - ${logs.size} appels", style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){ items(logs){ i -> Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.65f))){ Box(Modifier.padding(16.dp)){ Text(i) } } } }
    }
}
