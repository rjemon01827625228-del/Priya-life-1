package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.ui.storage.AgencyInviteMessage
import com.example.ui.storage.SocialAgencyStorage
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPinkBright

data class ChatConversation(
  val id: String,
  val name: String,
  val avatarRes: Int,
  val flagEmoji: String,
  val isItalic: Boolean = false,
  val lastMessage: String = "Hello! Are you online?",
  val unreadCount: Int = 1
)

@Composable
fun MessageInboxScreen(
  onChatClick: (ChatConversation) -> Unit,
  onHelplineClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeId = SocialAgencyStorage.getActiveUserId(context)
  var pendingInvites by remember {
    mutableStateOf(SocialAgencyStorage.getPendingInvitesForUser(context, activeId))
  }
  var selectedFilter by remember { mutableStateOf("All") }

  val chats = remember {
    listOf(
      ChatConversation("1", "me picci", R.drawable.img_streamer_girl, "🇧🇩", isItalic = true),
      ChatConversation("2", "PRIYA", R.drawable.img_streamer_girl, "🇨🇮", isItalic = false),
      ChatConversation("3", "Cyber Queen", R.drawable.img_profile_max, "🇧🇩", isItalic = false)
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding(),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 90.dp)
  ) {
    // 1. "Message" Glowing Title
    item {
      Text(
        text = "Message",
        color = Color(0xFF64D2FF),
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    // 2. Status Filters: [All], [Online], [Unread]
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // [All] Pill
        StatusFilterPill(
          title = "All",
          isSelected = selectedFilter == "All",
          gradientBorder = Brush.horizontalGradient(listOf(Color(0xFF0077B6), NeonCyan)),
          onClick = { selectedFilter = "All" }
        )

        // [Online] Pill
        StatusFilterPill(
          title = "Online",
          isSelected = selectedFilter == "Online",
          gradientBorder = Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0091FF))),
          onClick = { selectedFilter = "Online" }
        )

        // [Unread] Pill
        StatusFilterPill(
          title = "Unread",
          isSelected = selectedFilter == "Unread",
          gradientBorder = Brush.horizontalGradient(listOf(NeonPink, Color(0xFF7A00CC))),
          onClick = { selectedFilter = "Unread" }
        )
      }
    }

    // 3. Top Pinned Card: "OFFICIAL HELPLINE"
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(32.dp))
          .border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
              listOf(Color(0xFFFF3366), Color(0xFF00C0FF), Color(0xFFFF3366))
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .background(
            Brush.horizontalGradient(
              listOf(
                Color(0xFFCC1144),
                Color(0xFF225588),
                Color(0xFFCC1144)
              )
            )
          )
          .clickable(onClick = onHelplineClick)
          .padding(horizontal = 10.dp, vertical = 8.dp)
          .testTag("official_helpline_card")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Circular Avatar (Support girl with headset)
          Image(
            painter = painterResource(id = R.drawable.img_streamer_girl),
            contentDescription = "Official Helpline",
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .border(1.5.dp, Color.White, CircleShape),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.width(16.dp))

          Text(
            text = "OFFICIAL HELPLINE",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    // Agency Invitations (if any pending for active user)
    if (pendingInvites.isNotEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "AGENCY INVITATIONS (${pendingInvites.size})",
            color = Color(0xFFFFD700),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )

          pendingInvites.forEach { invite ->
            AgencyInviteCard(
              invite = invite,
              onAccept = {
                SocialAgencyStorage.respondToInvitation(context, invite.inviteId, true)
                pendingInvites = SocialAgencyStorage.getPendingInvitesForUser(context, activeId)
                Toast.makeText(context, "Joined ${invite.agencyName} as Official Host!", Toast.LENGTH_SHORT).show()
              },
              onReject = {
                SocialAgencyStorage.respondToInvitation(context, invite.inviteId, false)
                pendingInvites = SocialAgencyStorage.getPendingInvitesForUser(context, activeId)
                Toast.makeText(context, "Invitation declined", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      }
    }

    // 4. Chat Rows
    items(chats.size) { index ->
      val chat = chats[index]
      ChatRowCard(
        chat = chat,
        onClick = { onChatClick(chat) },
        modifier = Modifier.testTag("chat_row_$index")
      )
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun StatusFilterPill(
  title: String,
  isSelected: Boolean,
  gradientBorder: Brush,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .border(
        width = if (isSelected) 2.dp else 1.2.dp,
        brush = gradientBorder,
        shape = RoundedCornerShape(20.dp)
      )
      .background(
        if (isSelected) {
          Brush.verticalGradient(listOf(Color(0xFF1E0A44), Color(0xFF0C0220)))
        } else {
          Brush.verticalGradient(listOf(Color(0xFF14022A), Color(0xFF090014)))
        }
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 24.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = Color.White,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun ChatRowCard(
  chat: ChatConversation,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .border(
        width = 1.6.dp,
        brush = Brush.horizontalGradient(
          listOf(
            NeonCyan.copy(alpha = 0.8f),
            Color(0xFF8A00D4),
            NeonPink.copy(alpha = 0.8f)
          )
        ),
        shape = RoundedCornerShape(28.dp)
      )
      .background(
        Brush.horizontalGradient(
          listOf(
            Color(0xFF16012B),
            Color(0xFF260144),
            Color(0xFF140024)
          )
        )
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 7.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left avatar
      Image(
        painter = painterResource(id = chat.avatarRes),
        contentDescription = chat.name,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .border(1.5.dp, NeonPink, CircleShape),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(14.dp))

      // Center username (with custom font style: italic for "me picci", bold for "PRIYA")
      Text(
        text = chat.name,
        color = Color.White,
        fontSize = if (chat.isItalic) 18.sp else 17.sp,
        fontStyle = if (chat.isItalic) FontStyle.Italic else FontStyle.Normal,
        fontWeight = if (chat.isItalic) FontWeight.Normal else FontWeight.Black,
        letterSpacing = if (chat.isItalic) 1.sp else 2.sp,
        modifier = Modifier.weight(1f)
      )

      // Right: Country Flag Badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF120022))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = chat.flagEmoji,
          fontSize = 26.sp
        )
      }
    }
  }
}

@Composable
private fun AgencyInviteCard(
  invite: AgencyInviteMessage,
  onAccept: () -> Unit,
  onReject: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(Color(0xFF1B033A))
      .border(
        width = 1.5.dp,
        brush = Brush.horizontalGradient(
          listOf(Color(0xFFFFD700), Color(0xFFFF26B9))
        ),
        shape = RoundedCornerShape(20.dp)
      )
      .padding(14.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(24.dp)
          )
          Column {
            Text(
              text = invite.agencyName,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Text(
              text = "Agency ID: ${invite.agencyId}",
              color = Color(0xFF00E5FF),
              fontSize = 12.sp
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "Official Invitation",
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Text(
        text = "You have been personally invited to join as an Official Broadcaster. Unlock exclusive gifts, monthly bonus pools, and 1-on-1 streamer coaching.",
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onReject,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White.copy(alpha = 0.7f)
          )
        ) {
          Text("Decline", fontSize = 13.sp)
        }

        Button(
          onClick = onAccept,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF26B9)
          )
        ) {
          Text("Accept & Join", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}
