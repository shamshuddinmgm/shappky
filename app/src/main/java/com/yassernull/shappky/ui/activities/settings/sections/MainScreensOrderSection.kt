package com.yassernull.shappky.ui.activities.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.core.preferences.MainScreensPrefs
import com.yassernull.shappky.ui.activities.main.AppsCategory
import com.yassernull.shappky.ui.components.SettingsHeader

@Composable
fun MainScreensOrderSection(sharedPreferences: SharedPreferences) {
  var order by remember { mutableStateOf(MainScreensPrefs.loadOrder(sharedPreferences)) }
  var visibility by remember {
    mutableStateOf(AppsCategory.entries.associateWith { MainScreensPrefs.isVisible(sharedPreferences, it) })
  }

  fun visibleCount(): Int = visibility.values.count { it }

  fun persistVisibility(category: AppsCategory, visible: Boolean) {
    visibility = visibility.toMutableMap().also { it[category] = visible }
    sharedPreferences.edit()
      .putBoolean(MainScreensPrefs.visibilityKey(category), visible)
      .apply()
  }

  fun move(index: Int, delta: Int) {
    val target = index + delta
    if (target !in order.indices) return
    val mutable = order.toMutableList()
    val item = mutable.removeAt(index)
    mutable.add(target, item)
    order = mutable
    MainScreensPrefs.saveOrder(sharedPreferences, order)
  }

  SettingsHeader(text = stringResource(R.string.settings_main_screens))
  Text(
    text = stringResource(R.string.settings_main_screens_summary),
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
    fontSize = 13.sp,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
  )

  order.forEachIndexed { index, category ->
    val title = stringResource(MainScreensPrefs.titleRes(category))
    val checked = visibility[category] == true
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text(
          text = stringResource(R.string.screen_order_position, index + 1),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
          fontSize = 12.sp,
        )
      }
      IconButton(
        onClick = { move(index, -1) },
        enabled = index > 0,
      ) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_screen_up))
      }
      IconButton(
        onClick = { move(index, 1) },
        enabled = index < order.lastIndex,
      ) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_screen_down))
      }
      Switch(
        checked = checked,
        onCheckedChange = { want ->
          if (want || visibleCount() > 1) {
            persistVisibility(category, want)
          }
        },
      )
    }
  }
}
