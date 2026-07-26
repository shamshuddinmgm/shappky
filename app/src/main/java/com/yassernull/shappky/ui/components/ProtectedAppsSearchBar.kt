package com.yassernull.shappky.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R

@Composable
fun ProtectedAppsSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  showUserApps: Boolean,
  onShowUserAppsChange: (Boolean) -> Unit,
  showSystemApps: Boolean,
  onShowSystemAppsChange: (Boolean) -> Unit,
  showPersistentApps: Boolean,
  onShowPersistentAppsChange: (Boolean) -> Unit,
  isMenuExpanded: Boolean,
  onToggleMenu: () -> Unit,
  onDismissMenu: () -> Unit,
  hasVisibleSelection: Boolean,
  onSelectAllVisible: () -> Unit,
  onDeselectAllVisible: () -> Unit,
) {
  val focusManager = LocalFocusManager.current

  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text(stringResource(R.string.search_apps)) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    trailingIcon = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = {
            if (hasVisibleSelection) onDeselectAllVisible() else onSelectAllVisible()
          },
        ) {
          Icon(
            imageVector = if (hasVisibleSelection) Icons.Filled.Deselect else Icons.Filled.SelectAll,
            contentDescription = stringResource(
              if (hasVisibleSelection) R.string.unselect_all else R.string.select_all,
            ),
          )
        }
        IconButton(onClick = onToggleMenu) {
          Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.filter_content_desc))
        }
        DropdownMenu(
          expanded = isMenuExpanded,
          onDismissRequest = onDismissMenu,
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.select_all)) },
            leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
            onClick = {
              onDismissMenu()
              onSelectAllVisible()
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.unselect_all)) },
            leadingIcon = { Icon(Icons.Filled.Deselect, contentDescription = null) },
            onClick = {
              onDismissMenu()
              onDeselectAllVisible()
            },
          )
          HorizontalDivider()
          DropdownMenuItem(
            text = { Text(stringResource(R.string.user_apps)) },
            trailingIcon = { Checkbox(checked = showUserApps, onCheckedChange = onShowUserAppsChange) },
            onClick = { onShowUserAppsChange(!showUserApps) },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.system_apps)) },
            trailingIcon = { Checkbox(checked = showSystemApps, onCheckedChange = onShowSystemAppsChange) },
            onClick = { onShowSystemAppsChange(!showSystemApps) },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.persistent_apps)) },
            trailingIcon = { Checkbox(checked = showPersistentApps, onCheckedChange = onShowPersistentAppsChange) },
            onClick = { onShowPersistentAppsChange(!showPersistentApps) },
          )
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
  )
}
