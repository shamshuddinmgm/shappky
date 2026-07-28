package com.yassernull.shappky.ui.activities.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.core.preferences.MainScreensPrefs
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.data.models.AppDetailedInfo
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.activities.main.logic.AppsList
import com.yassernull.shappky.ui.activities.main.logic.AppsListLogic
import com.yassernull.shappky.ui.activities.main.logic.AppsRamUsage
import kotlin.math.abs

/**
 * Category screens use a **single** LazyColumn (no HorizontalPager).
 * ADB gfxinfo showed pager page composition spikes (~150ms / 63% legacy jank)
 * because each swipe mounted a full app list mid-gesture.
 * Swipe-left/right changes the selected tab; only one list stays composed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
  apps: List<AppModel>,
  hasPermission: Boolean,
  isLoadingBackgroundApps: Boolean,
  showUserApps: Boolean,
  showSystemApps: Boolean,
  showPersistentApps: Boolean,
  showProtectedApps: Boolean,
  showServiceProcesses: Boolean,
  showAppTypeIcons: Boolean,
  initialSortMode: String,
  initialSortDescending: Boolean,
  sortByName: String,
  sortByRam: String,
  hiddenApps: Set<String>,
  onSelectAll: (selected: Boolean, limitToPackages: Set<String>?) -> Unit,
  onRefresh: () -> Unit,
  onToggleShowUserApps: () -> Unit,
  onToggleShowSystemApps: () -> Unit,
  onToggleShowPersistentApps: () -> Unit,
  onToggleShowProtectedApps: () -> Unit,
  onToggleShowServiceProcesses: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenDonate: () -> Unit,
  onKillSelected: () -> Unit,
  onToggleApp: (AppModel) -> Unit,
  onKillApp: (AppModel, Boolean) -> Unit,
  onApplySort: (sortMode: String, descending: Boolean) -> Unit,
  onLoadAllApps: (((List<AppModel>) -> Unit)) -> Unit,
  onSaveHiddenApps: (Set<String>) -> Unit,
  onFilterSaved: () -> Unit,
  onOpenTriggers: () -> Unit,
  onAppLongClick: (AppModel) -> Unit,
  selectedAppForInfo: AppDetailedInfo?,
  onDismissAppInfo: () -> Unit,
) {
  var showSortDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val prefs = remember {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  }

  var screenEpoch by remember { mutableIntStateOf(0) }
  DisposableEffect(prefs) {
    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key != null && (key.startsWith("showScreen") || key == "screenOrder")) {
        screenEpoch++
      }
    }
    prefs.registerOnSharedPreferenceChangeListener(listener)
    onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }

  val screens = remember(screenEpoch) { MainScreensPrefs.loadVisibleScreens(prefs, context) }
  var selectedPage by remember { mutableIntStateOf(0) }
  LaunchedEffect(screens.size, screenEpoch) {
    val last = (screens.size - 1).coerceAtLeast(0)
    if (selectedPage > last) selectedPage = last
  }

  DisposableEffect(Unit) {
    onDispose { AppsListLogic.pagerScrollInProgress = false }
  }

  val checklist = AllScreenChecklist(
    showUserApps = showUserApps,
    showSystemApps = showSystemApps,
    showPersistentApps = showPersistentApps,
    showProtectedApps = showProtectedApps,
    showServiceProcesses = showServiceProcesses,
  )

  val currentCategory = screens.getOrNull(selectedPage)?.first ?: AppsCategory.ALL
  val visibleCount by remember(currentCategory, checklist) {
    derivedStateOf { apps.filterForScreen(currentCategory, checklist).size }
  }
  val visibleSelectablePackages by remember(currentCategory, checklist) {
    derivedStateOf {
      apps.filterForScreen(currentCategory, checklist)
        .asSequence()
        .filter { !it.isProtected && !it.isServiceProcess }
        .map { it.packageName }
        .toSet()
    }
  }
  val hasSelection by remember {
    derivedStateOf { apps.any { it.isSelected } }
  }

  val density = LocalDensity.current
  val swipeThresholdPx = with(density) { 72.dp.toPx() }
  var dragOffset by remember { mutableFloatStateOf(0f) }
  val pageCount = screens.size.coerceAtLeast(1)
  val selectedPageState = rememberUpdatedState(selectedPage)

  val categorySwipeConnection = remember(pageCount, swipeThresholdPx) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Only claim clearly-horizontal gestures so LazyColumn keeps vertical scroll.
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (abs(available.x) <= abs(available.y) * 1.25f) return Offset.Zero
        AppsListLogic.pagerScrollInProgress = true
        val page = selectedPageState.value
        val next = dragOffset + available.x
        dragOffset = when {
          page <= 0 && next > 0f -> next * 0.35f
          page >= pageCount - 1 && next < 0f -> next * 0.35f
          else -> next
        }
        return Offset(available.x, 0f)
      }

      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (abs(available.x) < 0.5f) return Offset.Zero
        if (abs(available.x) <= abs(available.y)) return Offset.Zero
        AppsListLogic.pagerScrollInProgress = true
        dragOffset += available.x
        return Offset(available.x, 0f)
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
        val page = selectedPageState.value
        val dx = dragOffset
        when {
          dx <= -swipeThresholdPx && page < pageCount - 1 -> selectedPage = page + 1
          dx >= swipeThresholdPx && page > 0 -> selectedPage = page - 1
          available.x <= -1200f && page < pageCount - 1 -> selectedPage = page + 1
          available.x >= 1200f && page > 0 -> selectedPage = page - 1
        }
        dragOffset = 0f
        AppsListLogic.pagerScrollInProgress = false
        return if (abs(available.x) > abs(available.y)) {
          Velocity(available.x, 0f)
        } else {
          Velocity.Zero
        }
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        dragOffset = 0f
        AppsListLogic.pagerScrollInProgress = false
        return Velocity.Zero
      }
    }
  }

  Scaffold(
    topBar = {
      MainToolbar(
        appsCount = visibleCount,
        hasSelection = hasSelection,
        showUserApps = showUserApps,
        showSystemApps = showSystemApps,
        showPersistentApps = showPersistentApps,
        showProtectedApps = showProtectedApps,
        showServiceProcesses = showServiceProcesses,
        onOpenTriggers = onOpenTriggers,
        onSelectAll = { selected ->
          onSelectAll(selected, if (selected) visibleSelectablePackages else null)
        },
        onToggleShowUserApps = onToggleShowUserApps,
        onToggleShowSystemApps = onToggleShowSystemApps,
        onToggleShowPersistentApps = onToggleShowPersistentApps,
        onToggleShowProtectedApps = onToggleShowProtectedApps,
        onToggleShowServiceProcesses = onToggleShowServiceProcesses,
        onOpenSortDialog = { showSortDialog = true },
        onOpenFilterDialog = { showFilterDialog = true },
        onOpenSettings = onOpenSettings,
        onOpenDonate = onOpenDonate,
      )
    },
    floatingActionButton = {
      MainFab(
        hasSelection = hasSelection,
        onKillSelected = onKillSelected,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(padding),
    ) {
      if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.permission_denied),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 16.sp,
          )
        }
        return@Column
      }
      AppsRamUsage()

      if (screens.size > 1) {
        PrimaryScrollableTabRow(
          selectedTabIndex = selectedPage.coerceIn(0, screens.lastIndex),
          edgePadding = 8.dp,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
        ) {
          screens.forEachIndexed { index, (_, title) ->
            val selected = selectedPage == index
            Tab(
              selected = selected,
              onClick = {
                selectedPage = index
                dragOffset = 0f
              },
              selectedContentColor = MaterialTheme.colorScheme.primary,
              unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              text = {
                Text(
                  text = title,
                  maxLines = 1,
                  fontSize = 13.sp,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
              },
            )
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .nestedScroll(categorySwipeConnection)
          .graphicsLayer {
            // Cheap parallax hint — does not compose a second list.
            translationX = dragOffset * 0.18f
            alpha = 1f - (abs(dragOffset) / (swipeThresholdPx * 4f)).coerceIn(0f, 0.12f)
          },
      ) {
        AppsList(
          apps = apps,
          category = currentCategory,
          checklist = checklist,
          listKey = currentCategory.name,
          isLoadingBackgroundApps = isLoadingBackgroundApps,
          showAppTypeIcons = showAppTypeIcons,
          onRefresh = onRefresh,
          onToggleApp = onToggleApp,
          onKillApp = onKillApp,
          onAppLongClick = onAppLongClick,
        )
      }
    }
  }

  MainDialogs(
    showSortDialog = showSortDialog,
    onDismissSortDialog = { showSortDialog = false },
    initialSortMode = initialSortMode,
    initialSortDescending = initialSortDescending,
    sortByName = sortByName,
    sortByRam = sortByRam,
    onApplySort = { mode, desc ->
      onApplySort(mode, desc)
      showSortDialog = false
    },
    showFilterDialog = showFilterDialog,
    onDismissFilterDialog = { showFilterDialog = false },
    hiddenApps = hiddenApps,
    onLoadAllApps = onLoadAllApps,
    onSaveHiddenApps = onSaveHiddenApps,
    onFilterSaved = {
      showFilterDialog = false
      onFilterSaved()
    },
    selectedAppForInfo = selectedAppForInfo,
    onDismissAppInfo = onDismissAppInfo,
  )
}
