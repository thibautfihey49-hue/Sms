package com.calculatrice.messenger
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.os.Handler
import android.os.Looper

class CalculatorActivity : Activity() {
    private lateinit var tvDisplay: TextView
    private val currentInput = StringBuilder()
    // 🔑 CODE SECRET — change le avant de compiler !
    private val CORRECT_PIN = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        tvDisplay = findViewById(R.id.tvDisplay)
        updateDisplay()

        listOf(R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
               R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
               R.id.btn8 to "8", R.id.btn9 to "9", R.id.btnDot to ".").forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                currentInput.append(value)
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentInput.clear()
            updateDisplay()
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            val input = currentInput.toString()
            if (input == CORRECT_PIN) {
                startActivity(Intent(this, MessengerActivity::class.java))
                currentInput.clear()
                updateDisplay()
            } else {
                performFakeCalculation(input)
            }
        }

        findViewById<Button>(R.id.btnPlus).setOnClickListener { currentInput.append("+"); updateDisplay() }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { currentInput.append("-"); updateDisplay() }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { currentInput.append("×"); updateDisplay() }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { currentInput.append("÷"); updateDisplay() }
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isEmpty()) "0" else currentInput.toString()
    }

    private fun performFakeCalculation(input: String) {
        tvDisplay.text = "Erreur"
        currentInput.clear()
        Handler(Looper.getMainLooper()).postDelayed({ updateDisplay() }, 1500)
    }

    override fun onResume() {
        super.onResume()
        currentInput.clear()
        updateDisplay()
    }
}
