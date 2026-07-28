package com.yassernull.shappky.ui.activities.main

import com.yassernull.shappky.data.models.AppModel

enum class AppsCategory {
  ALL,
  USER,
  SYSTEM,
  PERSISTENT,
  PROTECTED,
  SERVICES,
}

data class AllScreenChecklist(
  val showUserApps: Boolean,
  val showSystemApps: Boolean,
  val showPersistentApps: Boolean,
  val showProtectedApps: Boolean,
  val showServiceProcesses: Boolean,
)

/**
 * Category screens ignore the More-menu checklist and always show their type.
 * Checklist filters apply only on [AppsCategory.ALL].
 */
fun List<AppModel>.filterForScreen(
  category: AppsCategory,
  checklist: AllScreenChecklist,
): List<AppModel> = when (category) {
  AppsCategory.ALL -> filter { app ->
    when {
      app.isServiceProcess -> checklist.showServiceProcesses
      app.isProtected && !checklist.showProtectedApps -> false
      app.isPersistentApp && !checklist.showPersistentApps -> false
      app.isSystemApp && !app.isPersistentApp && !checklist.showSystemApps -> false
      !app.isSystemApp && !app.isPersistentApp && !app.isServiceProcess && !checklist.showUserApps -> false
      else -> true
    }
  }
  AppsCategory.USER -> filter { !it.isServiceProcess && !it.isSystemApp && !it.isPersistentApp }
  AppsCategory.SYSTEM -> filter { !it.isServiceProcess && it.isSystemApp }
  AppsCategory.PERSISTENT -> filter { !it.isServiceProcess && it.isPersistentApp }
  AppsCategory.PROTECTED -> filter { !it.isServiceProcess && it.isProtected }
  AppsCategory.SERVICES -> filter { it.isServiceProcess }
}
