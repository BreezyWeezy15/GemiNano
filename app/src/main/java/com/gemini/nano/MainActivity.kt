package com.gemini.nano

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    companion object {
        private const val TEMPERATURE = 0.2f
        private const val TOP_K = 12
        private const val MAX_OUTPUT_TOKEN = 2000
        private const val CANDIDATE_COUNT = 3
    }
    private var model: GenerativeModel? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        recyclerView = findViewById(R.id.chatRecyclerView)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {
            val inputText = etMessage.text.toString().trim()
            if (inputText.isNotEmpty()) {
                addMessage(inputText, isUser = true)
                etMessage.setText("")
                lifecycleScope.launch {
                    generateContent(inputText)
                }
            }
        }
        initGenerativeModel()

    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = ChatMessage(text, isUser)
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun initGenerativeModel() {
        if (model == null) {

            val downloadConfig = DownloadConfig(object : DownloadCallback {
                override fun onDownloadDidNotStart(e: GenerativeAIException) {
                    super.onDownloadDidNotStart(e)
                    val msg = "onDownloadDidNotStart"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadCompleted() {
                    super.onDownloadCompleted()
                    val msg = "onDownloadCompleted"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadProgress(totalBytesDownloaded: Long) {
                    super.onDownloadProgress(totalBytesDownloaded)
                    val msg = "onDownloadProgress: $totalBytesDownloaded bytes downloaded"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
                    super.onDownloadFailed(failureStatus, e)
                    val msg = "onDownloadFailed: $failureStatus"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadStarted(bytesToDownload: Long) {
                    super.onDownloadStarted(bytesToDownload)
                    val msg = "onDownloadStarted: $bytesToDownload bytes"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onDownloadPending() {
                    super.onDownloadPending()
                    val msg = "onDownloadPending"
                    Log.d("Gemini Nano Log", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }
            })

            val generationConfig = generationConfig {
                context = this@MainActivity
                temperature = TEMPERATURE
                topK = TOP_K
                maxOutputTokens = MAX_OUTPUT_TOKEN
                candidateCount = CANDIDATE_COUNT
            }

            model = GenerativeModel(generationConfig, downloadConfig)
            lifecycleScope.launch {
                try {
                    model?.prepareInferenceEngine()
                } catch (e: GenerativeAIException) {
                    Log.e("Gemini Nano Log", "Failed to check model availability.", e)
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private suspend fun generateContent(request: String) {
        try {
            val response = model!!.generateContent(request)
            val aiMessage = ChatMessage(text = response.text ?: "No response", isUser = false)
            chatAdapter.addMessage(aiMessage)
        } catch (e: GenerativeAIException) {
            Log.e("TAG", "Error generating content", e)

            // Handle specific errors
            if (e.message?.contains("CONNECTION_ERROR") == true) {
                val errorMessage = ChatMessage(
                    text = "Failed to connect to AI Core service.",
                    isUser = false
                )
                chatAdapter.addMessage(errorMessage)
            } else {
                val errorMessage = ChatMessage(text = e.message ?: "Unknown error", isUser = false)
                chatAdapter.addMessage(errorMessage)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        model?.close()
    }

}