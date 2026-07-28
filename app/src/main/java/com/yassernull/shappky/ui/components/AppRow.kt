package com.yassernull.shappky.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.theme.SelectionAccent
import com.yassernull.shappky.ui.theme.SelectionAccentDeep
import com.yassernull.shappky.ui.theme.SelectionFill
import com.yassernull.shappky.ui.theme.SelectionFillLight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
  app: AppModel,
  showAppTypeIcons: Boolean,
  onToggle: () -> Unit,
  onKill: (Boolean) -> Unit,
  onLongClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val selectedFill = if (isDark) {
    SelectionFill.copy(alpha = 0.72f)
  } else {
    SelectionFillLight.copy(alpha = 1f)
  }
  val selectedBorder = if (isDark) SelectionAccent.copy(alpha = 0.90f) else SelectionAccentDeep.copy(alpha = 0.70f)
  val shape = RoundedCornerShape(10.dp)

  val primaryTextColor = if (app.isSelected) {
    if (isDark) Color(0xFFFFFFFF) else Color(0xFF3A0E16)
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  val secondaryTextColor = if (app.isSelected) {
    if (isDark) Color(0xFFFFD6DC) else Color(0xFF6E1524)
  } else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
  }
  val rippleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

  var showForceKillDialog by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 60.dp)
      .alpha(if (app.isProtected) 0.4f else 1f)
      .padding(horizontal = 6.dp, vertical = 3.dp)
      .clip(shape)
      .then(
        if (app.isSelected) {
          Modifier
            .background(selectedFill, shape)
            .border(1.dp, selectedBorder, shape)
        } else {
          Modifier.background(MaterialTheme.colorScheme.surface, shape)
        },
      )
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = rippleColor),
        onClick = {
          if (app.isProtected || app.isServiceProcess) {
            if (!app.isServiceProcess) showForceKillDialog = true
          } else {
            onToggle()
          }
        },
        onLongClick = { if (!app.isServiceProcess) onLongClick() },
      )
      .padding(horizontal = 6.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (app.isSelected) {
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(42.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(if (isDark) SelectionAccent else SelectionAccentDeep),
      )
      Spacer(Modifier.width(8.dp))
    }
    DrawableIcon(app.appIcon, cacheKey = app.packageName)
    Spacer(Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = app.appName,
        color = primaryTextColor,
        fontSize = 16.sp,
        lineHeight = 17.sp,
        fontWeight = if (app.isSelected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = app.packageName,
        color = secondaryTextColor,
        fontSize = 12.sp,
        lineHeight = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (showAppTypeIcons) {
          val icon = when {
            app.isServiceProcess -> Icons.Outlined.Memory
            app.isPersistentApp -> Icons.Outlined.PushPin
            app.isSystemApp -> Icons.Outlined.Settings
            else -> Icons.Outlined.Person
          }
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = secondaryTextColor,
          )
          Spacer(Modifier.width(4.dp))
        }
        Text(
          text = app.appRam,
          color = secondaryTextColor,
          fontSize = 12.sp,
          lineHeight = 13.sp,
        )
      }
    }
    if (!app.isProtected && !app.isSelected && !app.isServiceProcess) {
      IconButton(
        onClick = { onKill(false) },
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          Icons.Outlined.Cancel,
          contentDescription = stringResource(R.string.force_stop),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
  HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

  if (showForceKillDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showForceKillDialog = false },
      title = { androidx.compose.material3.Text(stringResource(R.string.force_kill_protected_title)) },
      text = { androidx.compose.material3.Text(stringResource(R.string.force_kill_protected_message)) },
      confirmButton = {
        androidx.compose.material3.TextButton(onClick = {
          showForceKillDialog = false
          onKill(true)
        }) {
          androidx.compose.material3.Text(stringResource(R.string.yes))
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { showForceKillDialog = false }) {
          androidx.compose.material3.Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}
