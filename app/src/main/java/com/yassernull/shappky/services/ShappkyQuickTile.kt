package com.yassernull.shappky.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ShellManager
import java.util.concurrent.Executors

class ShappkyQuickTile : TileService() {
  override fun onStartListening() {
    super.onStartListening()
    val tile = qsTile ?: return
    val isRunning = ShappkyService.isRunning(this)
    tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
    tile.label = getString(R.string.shappky_service)
    tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.updateTile()
  }

  override fun onClick() {
    super.onClick()
    val tile = qsTile ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show()
      return
    }

    if (tile.state == Tile.STATE_INACTIVE) {
      val handler = Handler(Looper.getMainLooper())
      val executor = Executors.newSingleThreadExecutor()
      val shellManager = ShellManager(this, handler, executor)
      try {
        shellManager.checkShellPermissions()
        if (!shellManager.hasAnyShellPermission()) {
          Toast.makeText(this, getString(R.string.shell_permission_required), Toast.LENGTH_LONG).show()
          return
        }
      } finally {
        shellManager.removeShizukuPermissionListener()
        executor.shutdown()
      }
      startForegroundService(Intent(this, ShappkyService::class.java))
      tile.state = Tile.STATE_ACTIVE
      tile.label = getString(R.string.shappky_service)
    } else {
      stopService(Intent(this, ShappkyService::class.java))
      tile.state = Tile.STATE_INACTIVE
      tile.label = getString(R.string.shappky_service)
    }
    tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
    tile.updateTile()
  }
}
