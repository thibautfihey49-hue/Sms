package com.thibautfihey.sms.ui
import android.net.Uri; import android.provider.ContactsContract; import android.provider.Telephony
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items; import androidx.compose.foundation.shape.CircleShape; import androidx.compose.foundation.shape.RoundedCornerShape; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp; import androidx.navigation.NavController

@Composable fun DialPadScreenGlass(nav: NavController){
    var number by remember{ mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally){
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.9f))){ Box(Modifier.padding(20.dp)){ Text(number.ifEmpty{"Numéro"}, fontSize=32.sp, fontWeight=FontWeight.Light) } }
        Spacer(Modifier.height(24.dp))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach{ row-> Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly){ row.forEach{ d-> ElevatedButton(onClick={number+=d}, shape=CircleShape, modifier=Modifier.size(84.dp), colors=ButtonDefaults.elevatedButtonColors(containerColor=Color.White.copy(0.95f))){ Text(d, fontSize=26.sp) } } }; Spacer(Modifier.height(12.dp)) }
        Button(onClick={ if(number.isNotEmpty()) try{ nav.navigate("chat/${Uri.encode(number)}") }catch(_:Exception){} }, Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7C6CFF))){ Text("SMS / Appeler") }
    }
}
data class ContactReal(val name:String, val number:String, val key:String)
@Composable fun ContactsScreenGlass(nav: NavController){
    val ctx=LocalContext.current; var contacts by remember{ mutableStateOf<List<ContactReal>>(emptyList()) }; var q by remember{ mutableStateOf("") }
    LaunchedEffect(Unit){ try{
        val cr=ctx.contentResolver; val cur=cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null,null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")
        val list=mutableListOf<ContactReal>(); val seen=mutableSetOf<String>(); cur?.use{ c-> val nIdx=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val numIdx=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER); while(c.moveToNext()){ val n=if(nIdx>=0)c.getString(nIdx)?: "" else ""; val num=if(numIdx>=0)c.getString(numIdx)?: "" else ""; val k=normalizeNum(num); if(k.length<7)continue; if(n.isNotEmpty()&&num.isNotEmpty()&&seen.add(k)) list.add(ContactReal(n,num,k)) } }; contacts=list }catch(_:Exception){} }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Contacts • ${contacts.size} réels", style=MaterialTheme.typography.titleLarge); OutlinedTextField(q,{q=it}, placeholder={Text("Rechercher contact ou numéro réel")}, modifier=Modifier.fillMaxWidth().padding(top=8.dp), shape=RoundedCornerShape(20.dp)); Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){ items(contacts.filter{it.name.contains(q,ignoreCase=true)||it.number.contains(q)}){ c-> Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.9f))){ Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){ Column{ Text(c.name, fontWeight=FontWeight.Medium); Text(c.number, style=MaterialTheme.typography.labelSmall, color=Color.Gray) }; TextButton(onClick={ try{ nav.navigate("chat/${Uri.encode(c.number)}") }catch(_:Exception){} }){Text("SMS")} } } } }
    }
}
@Composable fun ChatScreenGlass(number:String){
    val ctx=LocalContext.current; val decoded=Uri.decode(number); val targetKey=normalizeNum(decoded); val contactMap=remember{ buildContactMap(ctx.contentResolver) }; val displayName=contactMap[targetKey]?: decoded
    var messages by remember{ mutableStateOf<List<Pair<String,Boolean>>>(emptyList()) }; var text by remember{ mutableStateOf("") }
    LaunchedEffect(decoded){ try{
        val cr=ctx.contentResolver; val cur=cr.query(Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.TYPE, Telephony.Sms.DATE), null,null, Telephony.Sms.DATE+" ASC")
        val msgs=mutableListOf<Pair<String,Boolean>>(); cur?.use{ c-> val aIdx=c.getColumnIndex(Telephony.Sms.ADDRESS); val bIdx=c.getColumnIndex(Telephony.Sms.BODY); val tIdx=c.getColumnIndex(Telephony.Sms.TYPE); while(c.moveToNext()){ val addr=if(aIdx>=0)c.getString(aIdx)?: "" else ""; if(normalizeNum(addr)!=targetKey)continue; val body=if(bIdx>=0)c.getString(bIdx)?: "" else ""; val type=if(tIdx>=0)c.getInt(tIdx) else 1; msgs.add(body to (type==2)) } }; messages=msgs.takeLast(200) }catch(_:Exception){} }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.9f))){ Column(Modifier.padding(16.dp)){ Text(displayName, style=MaterialTheme.typography.titleMedium); Text(decoded, style=MaterialTheme.typography.labelSmall, color=Color.Gray); Text("${messages.size} SMS groupés", style=MaterialTheme.typography.labelSmall, color=Color(0xFF7C6CFF)) } }
        Spacer(Modifier.height(8.dp)); LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(8.dp)){ items(messages){ (body, isMe)-> Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=if(isMe) Color(0xFF7C6CFF).copy(0.9f) else Color.White.copy(0.95f)), modifier=Modifier.fillMaxWidth(0.85f).let{ if(isMe) it.align(Alignment.End) else it }){ Box(Modifier.padding(12.dp)){ Text(body, color=if(isMe) Color.White else Color.Black) } } } }
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically){ OutlinedTextField(value=text, onValueChange={text=it}, placeholder={Text("Message à $displayName")}, modifier=Modifier.weight(1f), shape=RoundedCornerShape(20.dp)); Spacer(Modifier.width(8.dp)); Button(onClick={ try{ val sm=android.telephony.SmsManager.getDefault(); sm.sendTextMessage(decoded, null, text, null, null); messages=messages+(text to true); text="" }catch(_:Exception){} }, shape=CircleShape, modifier=Modifier.size(56.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7C6CFF))){ Text("➤") } }
    }
}
