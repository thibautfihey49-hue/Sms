package com.thibautfihey.sms.receiver
import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.provider.Telephony
class SmsDeliverReceiver: BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent){ try{ Telephony.Sms.Intents.getMessagesFromIntent(i) }catch(_:Exception){} } }
