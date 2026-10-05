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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPinkBright
import com.example.ui.theme.TextMuted

data class FeedPost(
  val id: String,
  val authorName: String,
  val authorAvatarRes: Int,
  val mediaRes: Int,
  val caption: String = "",
  val initialLikes: Int = 342,
  val commentCount: Int = 28
)

@Composable
fun SquareFeedScreen(
  onAddNewPost: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("Square") }

  val posts = remember {
    listOf(
      FeedPost(
        id = "1",
        authorName = "Mr Max",
        authorAvatarRes = R.drawable.img_profile_max,
        mediaRes = R.drawable.img_sad_mask,
        caption = "Moody cyberpunk nights 🌃✨",
        initialLikes = 520,
        commentCount = 42
      ),
      FeedPost(
        id = "2",
        authorName = "Mr Max",
        authorAvatarRes = R.drawable.img_profile_max,
        mediaRes = R.drawable.img_sad_mask,
        caption = "Behind every mask is another story...",
        initialLikes = 318,
        commentCount = 19
      )
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding(),
    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp)
  ) {
    // 1. Top Header: Neon Pill bar containing Square, Follow, Topics, Search, and Add
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
              listOf(
                NeonPinkBright,
                NeonCyan,
                Color(0xFF9D00FF),
                NeonPinkBright
              )
            ),
            shape = RoundedCornerShape(24.dp)
          )
          .background(Color(0xFF140026))
          .padding(horizontal = 6.dp, vertical = 5.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Left Tabs: Square, Follow, Topics
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // "Square" Active Neon Pill
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .border(
                  width = 1.2.dp,
                  brush = Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0091FF))),
                  shape = RoundedCornerShape(18.dp)
                )
                .background(
                  if (selectedTab == "Square") Color(0xFF0077B6).copy(alpha = 0.6f)
                  else Color.Transparent
                )
                .clickable { selectedTab = "Square" }
                .padding(horizontal = 14.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Square",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // "Follow" Tab
            Text(
              text = "Follow",
              color = if (selectedTab == "Follow") Color.White else TextMuted,
              fontSize = 14.sp,
              fontWeight = if (selectedTab == "Follow") FontWeight.Bold else FontWeight.Medium,
              modifier = Modifier
                .clickable { selectedTab = "Follow" }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            )

            // "Topics" Tab
            Text(
              text = "Topics",
              color = if (selectedTab == "Topics") Color.White else TextMuted,
              fontSize = 14.sp,
              fontWeight = if (selectedTab == "Topics") FontWeight.Bold else FontWeight.Medium,
              modifier = Modifier
                .clickable { selectedTab = "Topics" }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            )
          }

          // Right Icons: Search & Add (+)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F013B))
                .clickable { },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
            }

            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.2.dp, NeonCyan, RoundedCornerShape(8.dp))
                .background(Color(0xFF1F013B))
                .clickable(onClick = onAddNewPost),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Post",
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    // 2. Feed Cards
    items(posts.size) { index ->
      SquareFeedCard(
        post = posts[index],
        modifier = Modifier.testTag("feed_card_$index")
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun SquareFeedCard(
  post: FeedPost,
  modifier: Modifier = Modifier
) {
  var isLiked by remember { mutableStateOf(false) }
  var likeCount by remember { mutableIntStateOf(post.initialLikes) }
  var isBookmarked by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(
        width = 1.8.dp,
        brush = Brush.verticalGradient(
          listOf(
            NeonPink,
            Color(0xFF8A00D4),
            NeonCyan.copy(alpha = 0.8f)
          )
        ),
        shape = RoundedCornerShape(18.dp)
      )
      .background(
        Brush.verticalGradient(
          listOf(
            Color(0xFF1A0133),
            Color(0xFF0F0022)
          )
        )
      )
      .padding(12.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header: Profile picture + Username ("Mr Max")
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Image(
          painter = painterResource(id = post.authorAvatarRes),
          contentDescription = post.authorName,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(1.5.dp, NeonCyan, CircleShape),
          contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
          text = post.authorName,
          color = Color.White,
          fontSize = 17.sp,
          fontWeight = FontWeight.Black
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Media Preview Box (Full-width illustration container)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.22f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF222226))
      ) {
        Image(
          painter = painterResource(id = post.mediaRes),
          contentDescription = "Post media preview",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Interactive Action Bar below preview: Like, Comment, Share
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Heart / Like
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
              isLiked = !isLiked
              likeCount += if (isLiked) 1 else -1
            }
          ) {
            Icon(
              imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = "Like",
              tint = if (isLiked) LiveRed else Color.White,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$likeCount",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Comment
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { }
          ) {
            Icon(
              imageVector = Icons.Filled.ChatBubbleOutline,
              contentDescription = "Comments",
              tint = NeonCyan,
              modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${post.commentCount}",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }

          // Share
          Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = "Share",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
              .size(19.dp)
              .clickable { }
          )
        }

        // Bookmark
        Icon(
          imageVector = Icons.Filled.Bookmark,
          contentDescription = "Bookmark",
          tint = if (isBookmarked) NeonPinkBright else Color.White.copy(alpha = 0.5f),
          modifier = Modifier
            .size(20.dp)
            .clickable { isBookmarked = !isBookmarked }
        )
      }
    }
  }
}
