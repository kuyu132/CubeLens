package com.cubelens.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val title: String,
  val description: String,
  val emoji: String,
)

private val pages = listOf(
  OnboardingPage(
    title = "Point & Scan",
    description = "Aim your camera at any face of the Rubik's cube. CubeLens will detect the colors automatically using real-time HSV analysis.",
    emoji = "📷",
  ),
  OnboardingPage(
    title = "Scan All 6 Faces",
    description = "Rotate the cube and scan each face one by one. The app tracks which faces you've captured and guides you through the process.",
    emoji = "🎲",
  ),
  OnboardingPage(
    title = "Get Your Solution",
    description = "CubeLens solves the cube using the Kociemba two-phase algorithm and shows you each move step by step with a 3D preview.",
    emoji = "🧩",
  ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
  onCompleted: () -> Unit,
) {
  val pagerState = rememberPagerState(pageCount = { pages.size })
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
    ) { page ->
      val item = pages[page]
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = item.emoji,
          style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(24.dp))
        Text(
          text = item.title,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }

    // Dots indicator
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(pages.size) { index ->
        val isSelected = pagerState.currentPage == index
        Box(
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(if (isSelected) 10.dp else 8.dp)
            .clip(CircleShape)
            .background(
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            ),
        )
      }
    }

    // Navigation buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(
        onClick = onCompleted,
        modifier = Modifier.padding(start = 8.dp),
      ) {
        Text("Skip")
      }

      Button(
        onClick = {
          if (pagerState.currentPage < pages.size - 1) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          } else {
            onCompleted()
          }
        },
        modifier = Modifier.padding(end = 8.dp),
      ) {
        Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
      }
    }
  }
}
