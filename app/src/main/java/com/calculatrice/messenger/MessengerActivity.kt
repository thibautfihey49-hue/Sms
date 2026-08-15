package com.calculatrice.messenger
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MessengerActivity : Activity() {
    private lateinit var etIp: EditText
    private lateinit var etMessage: EditText
    private lateinit var llMessages: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnSend: Button
    private val service = MessengerService.get()
    private var isConnected = false
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        etIp = findViewById(R.id.etIp)
        etMessage = findViewById(R.id.etMessage)
        llMessages = findViewById(R.id.llMessages)
        tvStatus = findViewById(R.id.tvStatus)
        btnConnect = findViewById(R.id.btnConnect)
        btnSend = findViewById(R.id.btnSend)

        startServer()

        btnConnect.setOnClickListener {
            val ip = etIp.text.toString().trim()
            if (ip.isNotEmpty() && !isConnected) connectTo(ip)
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty() && isConnected) {
                if (service.sendMessage(text)) {
                    addMessage(text, true)
                    etMessage.text.clear()
                }
            }
        }

        findViewById<Button>(R.id.btnExit).setOnClickListener {
            service.closeAll()
            finish()
            System.gc()
        }
    }

    private fun startServer() {
        tvStatus.text = "🟢 En attente de connexion..."
        service.startServer(
            onMessage = { msg -> addMessage(msg, false) },
            onConnect = { isConnected = true; runOnUiThread { tvStatus.text = "🔒 Connecté et sécurisé" } },
            onDisconnect = { isConnected = false; runOnUiThread { tvStatus.text = "⚠️ Déconnecté" } }
        )
    }

    private fun connectTo(ip: String) {
        tvStatus.text = "🔄 Connexion en cours..."
        service.connectToServer(
            ip = ip,
            onMessage = { msg -> addMessage(msg, false) },
            onConnect = { isConnected = true; runOnUiThread { tvStatus.text = "🔒 Connecté et sécurisé" } },
            onDisconnect = { isConnected = false; runOnUiThread { tvStatus.text = "⚠️ Déconnecté" } }
        )
    }

    private fun addMessage(text: String, isSent: Boolean) {
        runOnUiThread {
            val msgLayout = LinearLayout(this)
            msgLayout.orientation = LinearLayout.VERTICAL
            msgLayout.setPadding(16, 8, 16, 8)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = if (isSent) Gravity.END else Gravity.START
            msgLayout.layoutParams = lp

            val bubble = TextView(this)
            bubble.text = text
            bubble.textSize = 16f
            bubble.setPadding(20, 12, 20, 12)
            bubble.setBackgroundColor(getColor(if (isSent) R.color.message_sent else R.color.message_received))
            bubble.setTextColor(android.graphics.Color.BLACK)

            val time = TextView(this)
            time.text = dateFormat.format(Date())
            time.textSize = 10f
            time.setPadding(0, 4, 0, 0)
            time.setTextColor(android.graphics.Color.GRAY)
            time.gravity = if (isSent) Gravity.END else Gravity.START

            msgLayout.addView(bubble)
            msgLayout.addView(time)
            llMessages.addView(msgLayout)
        }
    }

    override fun onDestroy() {
        service.closeAll()
        super.onDestroy()
    }

    override fun onBackPressed() {
        service.closeAll()
        finish()
        System.gc()
    }
}
