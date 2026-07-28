package com.yassernull.shappky.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsHeader(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.2.sp,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
  )
}
