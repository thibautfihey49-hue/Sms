package com.thibautfihey.sms.service
import android.app.Service
import android.content.Intent
import android.os.IBinder
class HeadlessSmsSendService: Service(){ override fun onBind(i: Intent?): IBinder? = null }
