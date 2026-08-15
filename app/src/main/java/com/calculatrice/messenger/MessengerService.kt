package com.calculatrice.messenger
import kotlinx.coroutines.*
import java.net.*
import java.io.*
import java.util.Base64

class MessengerService {
    private var server: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val PORT = 47899
    private var aesKey: ByteArray? = null

    companion object {
        var instance: MessengerService? = null
        fun get(): MessengerService {
            if (instance == null) instance = MessengerService()
            return instance!!
        }
    }

    fun startServer(onMessage: (String) -> Unit, onConnect: () -> Unit, onDisconnect: () -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        server = ServerSocket(PORT)
        try {
            val socket = server!!.accept()
            clientSocket = socket
            onConnect()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            aesKey = CryptoEngine.generateAesKey()
            writer.println(Base64.getEncoder().encodeToString(aesKey))
            while (true) {
                val msg = reader.readLine() ?: break
                val decrypted = CryptoEngine.decrypt(msg, aesKey!!)
                withContext(Dispatchers.Main) { onMessage(decrypted) }
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { withContext(Dispatchers.Main) { onDisconnect() }; closeAll() }
    }

    fun connectToServer(ip: String, onMessage: (String) -> Unit, onConnect: () -> Unit, onDisconnect: () -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, PORT)
            clientSocket = socket
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            val keyB64 = reader.readLine() ?: return@launch
            aesKey = Base64.getDecoder().decode(keyB64)
            onConnect()
            while (true) {
                val msg = reader.readLine() ?: break
                val decrypted = CryptoEngine.decrypt(msg, aesKey!!)
                withContext(Dispatchers.Main) { onMessage(decrypted) }
            }
        } catch (e: Exception) { withContext(Dispatchers.Main) { onMessage("❌ Impossible de se connecter") } }
        finally { withContext(Dispatchers.Main) { onDisconnect() }; closeAll() }
    }

    fun sendMessage(text: String): Boolean {
        val socket = clientSocket ?: return false
        val key = aesKey ?: return false
        return try {
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            writer.println(CryptoEngine.encrypt(text, key))
            true
        } catch (e: Exception) { false }
    }

    fun closeAll() {
        try { server?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        aesKey?.fill(0)
        aesKey = null
        instance = null
    }
}
