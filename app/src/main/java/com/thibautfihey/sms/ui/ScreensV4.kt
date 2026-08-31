package com.thibautfihey.sms.ui
import android.net.Uri; import android.provider.ContactsContract; import android.provider.Telephony; import android.provider.CallLog
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items; import androidx.compose.foundation.shape.RoundedCornerShape; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.unit.dp; import androidx.navigation.NavController; import androidx.compose.foundation.clickable

data class ThreadReal(val displayName: String, val rawNumber: String, val key: String, val last: String, val count: Int, val date: Long)
fun normalizeNum(n: String): String { val d = n.filter { it.isDigit() }; return if(d.length>9) d.takeLast(9) else d }
fun buildContactMap(cr: android.content.ContentResolver): Map<String,String>{
    val map=mutableMapOf<String,String>(); try{
        val cur=cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null,null,null)
        cur?.use{ c-> val nIdx=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val numIdx=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER); while(c.moveToNext()){ val name=if(nIdx>=0)c.getString(nIdx)?:continue else continue; val num=if(numIdx>=0)c.getString(numIdx)?:continue else continue; val k=normalizeNum(num); if(k.length>=7 &&!map.containsKey(k)) map[k]=name } }
    }catch(_:Exception){}; return map
}
@Composable fun MessagesListScreenGlassV4(nav: NavController){
    val ctx=LocalContext.current; var threads by remember{ mutableStateOf<List<ThreadReal>>(emptyList()) }; var loading by remember{ mutableStateOf(true) }
    LaunchedEffect(Unit){
        val contactMap=buildContactMap(ctx.contentResolver); val grouped=LinkedHashMap<String,ThreadReal>()
        try{
            val cr=ctx.contentResolver; val cur=cr.query(Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE), null,null, Telephony.Sms.DATE+" DESC")
            cur?.use{ c-> val aIdx=c.getColumnIndex(Telephony.Sms.ADDRESS); val bIdx=c.getColumnIndex(Telephony.Sms.BODY); val dIdx=c.getColumnIndex(Telephony.Sms.DATE)
                while(c.moveToNext()){ val raw=if(aIdx>=0)c.getString(aIdx)?:continue else continue; if(raw.isBlank())continue; val k=normalizeNum(raw); if(k.length<7)continue; if(grouped.containsKey(k)){ val o=grouped[k]!!; grouped[k]=o.copy(count=o.count+1); continue }
                    val body=if(bIdx>=0)c.getString(bIdx)?: "" else ""; val date=if(dIdx>=0)c.getLong(dIdx) else 0L; val realName=contactMap[k]?: raw; grouped[k]=ThreadReal(realName, raw, k, body, 1, date) } }
        }catch(e:Exception){ grouped["err"]=ThreadReal("Erreur", "", "err", e.message?:"READ_SMS requis",0,0) }
        threads=grouped.values.sortedByDescending{it.date}; loading=false
    }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Messages • ${threads.size} contacts regroupés", style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){ items(threads){ t-> Card(Modifier.fillMaxWidth().clickable{ nav.navigate("chat/${Uri.encode(t.rawNumber)}") }, shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.85f))){
            Column(Modifier.padding(16.dp)){ Text(t.displayName, style=MaterialTheme.typography.titleMedium); if(t.displayName!=t.rawNumber) Text(t.rawNumber, style=MaterialTheme.typography.labelSmall, color=Color.Gray); Spacer(Modifier.height(6.dp)); Text(t.last, maxLines=2, color=Color(0xFF666666)); if(t.count>1) Text("${t.count} messages", style=MaterialTheme.typography.labelSmall, color=Color(0xFF7C6CFF)) } } } }
    }
}
@Composable fun HistoryScreenGlassV4(onRequest:()->Unit){
    val ctx=LocalContext.current; val contactMap=remember{ buildContactMap(ctx.contentResolver) }; var logs by remember{ mutableStateOf<List<Pair<String,String>>>(emptyList()) }; var error by remember{ mutableStateOf(false) }
    LaunchedEffect(Unit){ try{
        val cr=ctx.contentResolver; val cur=cr.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION), null,null, CallLog.Calls.DATE+" DESC LIMIT 100")
        val list=mutableListOf<Pair<String,String>>(); cur?.use{ c-> val nIdx=c.getColumnIndex(CallLog.Calls.CACHED_NAME); val numIdx=c.getColumnIndex(CallLog.Calls.NUMBER); val typeIdx=c.getColumnIndex(CallLog.Calls.TYPE); val durIdx=c.getColumnIndex(CallLog.Calls.DURATION)
            while(c.moveToNext()){ val cache=if(nIdx>=0)c.getString(nIdx) else null; val num=if(numIdx>=0)c.getString(numIdx)?: "" else ""; val k=normalizeNum(num); val real=if(!cache.isNullOrEmpty()) cache else contactMap[k]?: num; val type=if(typeIdx>=0)c.getInt(typeIdx) else 0; val dur=if(durIdx>=0)c.getString(durIdx)?: "0" else "0"; val t=when(type){1->"Entrant";2->"Sortant";3->"Manqué";else->"Autre"}; list.add(real to "$num • $t • ${dur}s") } }; logs=list }catch(_:Exception){ error=true } }
    Column(Modifier.fillMaxSize().padding(16.dp)){ Text("Appels • ${logs.size} regroupés par contact", style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp))
        if(error){ Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE4E4))){ Column(Modifier.padding(16.dp)){ Button(onClick=onRequest, shape=RoundedCornerShape(50)){ Text("Autoriser Journal d'appels") } } } }
        else { LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){ items(logs){ (name, detail)-> Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(0.85f))){ Column(Modifier.padding(14.dp)){ Text(name, style=MaterialTheme.typography.titleSmall); Text(detail, style=MaterialTheme.typography.labelSmall, color=Color.Gray) } } } } }
    }
}
