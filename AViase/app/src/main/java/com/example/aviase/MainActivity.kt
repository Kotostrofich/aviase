package com.example.aviase

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var flightTable: TableLayout
    private lateinit var timer: Timer
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "FlightApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            flightTable = findViewById(R.id.flightTable)
            loadFlights()
            startTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Ошибка инициализации приложения", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadFlights() {
        try {
            runOnUiThread {
                flightTable.removeAllViews()
                createHeaderRow()
            }

            val flights = loadFlightsFromAssets("flights.txt")
            if (flights.isEmpty()) {
                showErrorMessage("Нет данных о рейсах")
                return
            }

            runOnUiThread {
                flights.forEach { flight ->
                    val row = TableRow(this).apply {
                        layoutParams = TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    listOf(flight.number, flight.destination, flight.time).forEach { text ->
                        row.addView(createTextView(text, Color.WHITE, Typeface.NORMAL))
                    }

                    row.addView(createTextView(flight.status, flight.statusColor, Typeface.NORMAL))
                    flightTable.addView(row)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading flights: ${e.message}")
            showErrorMessage("Ошибка загрузки данных")
        }
    }

    private fun showErrorMessage(message: String) {
        runOnUiThread {
            val errorRow = TableRow(this).apply {
                addView(createTextView(message, Color.RED, Typeface.BOLD).apply {
                    layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    ).also {
                        (it as TableRow.LayoutParams).span = 4
                    }
                })
            }
            flightTable.addView(errorRow)
        }
    }

    private fun createTextView(text: String, color: Int, typeface: Int): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(color)
            setTypeface(null, typeface)
            setPadding(16, 16, 16, 16)
            layoutParams = TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
    }

    private fun createHeaderRow() {
        val headerRow = TableRow(this).apply {
            setBackgroundColor(Color.parseColor("#FF333333"))
        }

        listOf("РЕЙС", "НАПРАВЛЕНИЕ", "ВРЕМЯ", "СТАТУС").forEach { title ->
            headerRow.addView(createTextView(title, Color.WHITE, Typeface.BOLD))
        }

        flightTable.addView(headerRow)
    }

    private fun loadFlightsFromAssets(filename: String): List<Flight> {
        return try {
            if (assets == null) {
                Log.e(TAG, "Assets manager is null")
                return emptyList()
            }

            val files = assets.list("") ?: emptyArray()
            if (!files.contains(filename)) {
                Log.e(TAG, "File $filename not found in assets")
                return emptyList()
            }

            assets.open(filename).bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        line.split(";").takeIf { it.size >= 5 }?.let { parts ->
                            Flight(
                                number = parts[0].trim(),
                                destination = parts[1].trim(),
                                time = parts[2].trim(),
                                status = parts[3].trim(),
                                statusColor = when (parts[4].trim().lowercase()) {
                                    "red" -> Color.RED
                                    "green" -> Color.GREEN
                                    "yellow" -> Color.YELLOW
                                    else -> Color.WHITE
                                }
                            )
                        }
                    }
                    .toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading flights file: ${e.message}")
            emptyList()
        }
    }

    private fun startTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    loadFlights()
                } catch (e: Exception) {
                    Log.e(TAG, "Timer error: ${e.message}")
                }
            }
        }, 5000, 5000)
    }

    override fun onPause() {
        super.onPause()
        try {
            timer.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling timer: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            timer.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling timer: ${e.message}")
        }
    }

    data class Flight(
        val number: String,
        val destination: String,
        val time: String,
        val status: String,
        val statusColor: Int
    )
}