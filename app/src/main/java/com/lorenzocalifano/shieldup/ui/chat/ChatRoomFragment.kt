package com.lorenzocalifano.shieldup.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.MessageDto
import com.lorenzocalifano.shieldup.utils.SessionManager

class ChatRoomFragment : Fragment(R.layout.fragment_chat_room) {

    private val repository = FirebaseRepository()

    private lateinit var sessionManager: SessionManager
    private lateinit var messagesContainer: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private var chatId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        chatId = arguments?.getString("chatId") ?: ""

        val title = view.findViewById<TextView>(R.id.txtChatRoomTitle)
        title.text = arguments?.getString("chatTitle") ?: "Chat"

        view.findViewById<TextView>(R.id.btnBackChat).setOnClickListener {
            findNavController().popBackStack()
        }

        messagesContainer = view.findViewById(R.id.messagesContainer)
        messageInput = view.findViewById(R.id.etMessage)
        sendButton = view.findViewById(R.id.btnSendMessage)

        if (chatId.isEmpty()) {
            Toast.makeText(requireContext(), "Chat non valida", Toast.LENGTH_LONG).show()
            return
        }

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        repository.loadMessages(
            chatId = chatId,
            onSuccess = { messages ->
                if (!isAdded) return@loadMessages

                messagesContainer.removeAllViews()

                if (messages.isEmpty()) {
                    messagesContainer.addView(createInfoText("Nessun messaggio"))
                    return@loadMessages
                }

                messages.forEach { message ->
                    messagesContainer.addView(createMessageView(message))
                }
            },
            onError = {
                if (!isAdded) return@loadMessages
                Toast.makeText(requireContext(), "Errore caricamento messaggi", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        sendButton.isEnabled = false

        repository.sendMessage(
            chatId = chatId,
            senderId = sessionManager.getUserId(),
            senderName = sessionManager.getName(),
            text = text,
            onSuccess = {
                if (!isAdded) return@sendMessage

                messageInput.text.clear()
                sendButton.isEnabled = true
                loadMessages()
            },
            onError = {
                if (!isAdded) return@sendMessage

                sendButton.isEnabled = true
                Toast.makeText(requireContext(), "Errore invio messaggio", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun createMessageView(message: MessageDto): TextView {
        val isMine = message.senderId == sessionManager.getUserId()

        return TextView(requireContext()).apply {
            text = "${message.senderName}: ${message.text}"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ContextCompat.getDrawable(
                requireContext(),
                if (isMine) R.drawable.bg_purple_button else R.drawable.bg_gray_button
            )

            if (isMine) {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    if (isMine) dp(48) else 0,
                    0,
                    if (isMine) 0 else dp(48),
                    dp(10)
                )
            }
        }
    }

    private fun createInfoText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            textSize = 16f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(12), 0, dp(12))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}