package com.cubelens.ui.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cubelens.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val titleRes: Int,
  val descriptionRes: Int,
  val emoji: String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
  onCompleted: () -> Unit,
) {
  val pages = listOf(
    OnboardingPage(R.string.onboarding_title_0, R.string.onboarding_desc_0, "📷"),
    OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_desc_1, "🎲"),
    OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_desc_2, "🧩"),
  )
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
          text = stringResource(item.titleRes),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = stringResource(item.descriptionRes),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }

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
              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            ),
        )
      }
    }

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
        Text(stringResource(R.string.onboarding_skip))
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
        Text(
          if (pagerState.currentPage < pages.size - 1) {
            stringResource(R.string.onboarding_next)
          } else {
            stringResource(R.string.onboarding_start)
          },
        )
      }
    }
  }
}
