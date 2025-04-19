package com.gemini.nano

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private var model: GenerativeModel? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
       if(model == null){

           val generationConfig = generationConfig {
               context = this@MainActivity
               temperature = 0.2f
               topK = 16
               maxOutputTokens = 256
           }

           model = GenerativeModel(generationConfig)
       }
    }
    private suspend fun generateContent(request: String) {
        try {

           val response = model!!.generateContent(request)
            val aiMessage = ChatMessage(text = response.text ?: "No response", isUser = false)
            chatAdapter.addMessage(aiMessage)  // Add AI message
        } catch (e: GenerativeAIException) {
            val errorMessage = ChatMessage(text = e.message ?: "Unknown error", isUser = false)
            chatAdapter.addMessage(errorMessage)
            Log.d("TAG","Error " + e.message)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        model?.close()
    }

}
