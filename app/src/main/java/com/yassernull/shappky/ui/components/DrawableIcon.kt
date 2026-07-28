package com.yassernull.shappky.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/** Process-wide icon cache — avoids re-decoding drawables on every pager page compose. */
object AppIconBitmapCache {
  private val cache = object : LruCache<String, ImageBitmap>(96) {}

  fun get(cacheKey: String, drawable: Drawable, sizePx: Int): ImageBitmap? {
    val key = "$cacheKey@$sizePx"
    cache.get(key)?.let { return it }
    val bmp = runCatching {
      drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap()
    }.getOrNull() ?: return null
    cache.put(key, bmp)
    return bmp
  }
}

@Composable
fun DrawableIcon(
  drawable: Drawable,
  modifier: Modifier = Modifier.size(48.dp),
  cacheKey: String? = null,
) {
  val density = LocalDensity.current
  val sizePx = with(density) { 48.dp.roundToPx().coerceAtLeast(1) }
  val bitmap = remember(cacheKey ?: drawable, sizePx) {
    if (cacheKey != null) {
      AppIconBitmapCache.get(cacheKey, drawable, sizePx)
    } else {
      runCatching {
        drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap()
      }.getOrNull()
    }
  }
  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = null,
      modifier = modifier,
    )
  }
}
