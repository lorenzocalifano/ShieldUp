package com.lorenzocalifano.shieldup.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.ChatDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private val repository = FirebaseRepository()
    private lateinit var sessionManager: SessionManager
    private lateinit var chatsContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        chatsContainer = view.findViewById(R.id.chatsContainer)

        loadChats()
    }

    override fun onResume() {
        super.onResume()
        if (::chatsContainer.isInitialized) {
            loadChats()
        }
    }

    private fun loadChats() {
        chatsContainer.removeAllViews()
        chatsContainer.addView(createInfoText("Caricamento chat..."))

        repository.loadChatsForUser(
            userId = sessionManager.getUserId(),
            role = sessionManager.getRole(),
            onSuccess = { chats ->
                if (!isAdded) return@loadChatsForUser

                chatsContainer.removeAllViews()

                if (chats.isEmpty()) {
                    chatsContainer.addView(createInfoText("Nessuna chat attiva"))
                    return@loadChatsForUser
                }

                chats.forEach { chat ->
                    chatsContainer.addView(createChatCard(chat))
                }
            },
            onError = {
                if (!isAdded) return@loadChatsForUser
                chatsContainer.removeAllViews()
                chatsContainer.addView(createInfoText("Errore caricamento chat"))
            }
        )
    }

    private fun createChatCard(chat: ChatDto): View {
        val otherName = if (sessionManager.getRole() == "PSYCHOLOGIST") {
            chat.userName
        } else {
            chat.psychologistName
        }

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gray_button)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(14))
            }

            setOnClickListener {
                val bundle = Bundle().apply {
                    putString("chatId", chat.id)
                    putString("chatTitle", otherName)
                }
                findNavController().navigate(R.id.chatRoomFragment, bundle)
            }
        }

        val title = TextView(requireContext()).apply {
            text = otherName
            textSize = 20f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "${chat.lastMessage} · ${formatTime(chat.lastMessageAt)}"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(6), 0, 0)
        }

        val delete = Button(requireContext()).apply {
            text = "Elimina chat"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.emergency_red))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gray_button)
            setOnClickListener {
                repository.deleteChat(
                    chatId = chat.id,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Chat eliminata", Toast.LENGTH_SHORT).show()
                        loadChats()
                    },
                    onError = {
                        Toast.makeText(requireContext(), "Errore eliminazione chat", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        card.addView(title)
        card.addView(subtitle)
        card.addView(delete)

        return card
    }

    private fun createInfoText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            textSize = 16f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(12), 0, dp(12))
        }
    }

    private fun formatTime(time: Long): String {
        if (time <= 0L) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}