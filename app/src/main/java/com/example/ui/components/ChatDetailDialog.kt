package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.ChatConversation
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPinkBright

data class DirectChatMessage(
  val sender: String,
  val text: String,
  val isMe: Boolean
)

@Composable
fun ChatDetailDialog(
  chat: ChatConversation,
  onDismiss: () -> Unit
) {
  var messageInput by remember { mutableStateOf("") }
  val messageList = remember {
    mutableStateListOf(
      DirectChatMessage(chat.name, "Hey! Welcome to my neon space 🌸", isMe = false),
      DirectChatMessage("Me", "Thanks! Great stream today!", isMe = true),
      DirectChatMessage(chat.name, "Thank you so much! Are you coming to the voice party tonight?", isMe = false)
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0F0024), Color(0xFF1B0038), Color(0xFF0A0018))
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
      ) {
        // Top Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF140129))
            .border(
              1.dp,
              Brush.horizontalGradient(listOf(NeonCyan, NeonPink)),
              RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Image(
            painter = painterResource(id = chat.avatarRes),
            contentDescription = null,
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .border(1.5.dp, NeonPink, CircleShape),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = chat.name,
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Online ${chat.flagEmoji}",
              color = NeonCyan,
              fontSize = 11.sp
            )
          }
        }

        // Messages List
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(messageList) { msg ->
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
            ) {
              Box(
                modifier = Modifier
                  .clip(
                    RoundedCornerShape(
                      topStart = 16.dp,
                      topEnd = 16.dp,
                      bottomStart = if (msg.isMe) 16.dp else 4.dp,
                      bottomEnd = if (msg.isMe) 4.dp else 16.dp
                    )
                  )
                  .border(
                    1.dp,
                    if (msg.isMe) NeonCyan else NeonPink,
                    RoundedCornerShape(
                      topStart = 16.dp,
                      topEnd = 16.dp,
                      bottomStart = if (msg.isMe) 16.dp else 4.dp,
                      bottomEnd = if (msg.isMe) 4.dp else 16.dp
                    )
                  )
                  .background(
                    if (msg.isMe) {
                      Brush.horizontalGradient(listOf(Color(0xFF0055AA), Color(0xFF0077B6)))
                    } else {
                      Brush.horizontalGradient(listOf(Color(0xFF330055), Color(0xFF550077)))
                    }
                  )
                  .padding(horizontal = 14.dp, vertical = 10.dp)
              ) {
                Text(
                  text = msg.text,
                  color = Color.White,
                  fontSize = 14.sp
                )
              }
            }
          }
        }

        // Message Input Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("Type a message...", color = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier
              .weight(1f)
              .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = NeonPink,
              focusedContainerColor = Color(0xFF17002C),
              unfocusedContainerColor = Color(0xFF120024)
            )
          )

          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(listOf(NeonCyan, Color(0xFF0077B6)))
              )
              .clickable {
                if (messageInput.isNotBlank()) {
                  messageList.add(DirectChatMessage("Me", messageInput, true))
                  messageInput = ""
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
