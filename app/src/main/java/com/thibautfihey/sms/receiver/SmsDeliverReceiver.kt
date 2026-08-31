package com.thibautfihey.sms.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
class SmsDeliverReceiver: BroadcastReceiver(){
    override fun onReceive(c: Context, i: Intent){
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(i)
        // On laisse le système stocker, on ne fait rien pour ne pas crasher
    }
}
