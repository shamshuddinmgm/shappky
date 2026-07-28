package com.yassernull.shappky.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.ProcessInfo

@Composable
fun ProcessRow(
  process: ProcessInfo,
  nameColor: Color = MaterialTheme.colorScheme.onSurface,
  metaColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  val context = LocalContext.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
  ) {
    Text(
      text = process.name,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = nameColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.pid_format, process.pid),
        style = MaterialTheme.typography.bodySmall,
        color = metaColor,
      )
      Text(
        text = Formatter.formatFileSize(context, process.ramKb * 1024L),
        style = MaterialTheme.typography.bodySmall,
        color = metaColor,
      )
    }
  }
}
