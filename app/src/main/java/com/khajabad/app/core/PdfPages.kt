package com.khajabad.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Rasterises pages with the platform PdfRenderer.
 * PDFs live either in assets (bundled books) or in filesDir (imported books);
 * asset bundles are copied to cache on first use because PdfRenderer needs a file.
 */
object PdfPages {

    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "khajabad-pdf").apply { priority = Thread.NORM_PRIORITY - 1 }
    }.asCoroutineDispatcher()

    private val lock = Mutex()

    /** Byte-budgeted page cache so a few full-screen pages never blow the heap. */
    private val cache = object : LruCache<String, ImageBitmap>(26 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val handles = HashMap<String, Handle>()

    private class Handle(
        val renderer: PdfRenderer,
        val descriptor: ParcelFileDescriptor,
        val pages: Int,
    )

    suspend fun pageCount(context: Context, book: Book): Int = withContext(dispatcher) {
        lock.withLock {
            open(context, book)?.pages ?: 0
        }
    }

    suspend fun render(
        context: Context,
        book: Book,
        page: Int,
        targetWidth: Int = 1080,
    ): ImageBitmap? = withContext(dispatcher) {
        lock.withLock {
            val handle = open(context, book) ?: return@withLock null
            if (page !in 0 until handle.pages) return@withLock null

            val bucket = (targetWidth / 120).coerceIn(2, 16)
            val key = "${book.id}:$page:$bucket"
            cache.get(key)?.let { return@withLock it }

            runCatching {
                val p = handle.renderer.openPage(page)
                try {
                    val ratio = p.height.toFloat() / p.width.toFloat()
                    val width = (bucket * 120).coerceAtLeast(360)
                    val height = (width * ratio).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val image = bitmap.asImageBitmap()
                    cache.put(key, image)
                    image
                } finally {
                    p.close()
                }
            }.getOrNull()
        }
    }

    fun clearCache() {
        cache.evictAll()
    }

    private fun open(context: Context, book: Book): Handle? {
        handles[book.id]?.let { return it }
        val file = fileFor(context, book) ?: return null
        return runCatching {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            Handle(renderer, descriptor, renderer.pageCount).also { handles[book.id] = it }
        }.getOrNull()
    }

    private fun fileFor(context: Context, book: Book): File? {
        book.filePath?.let { path ->
            val f = File(path)
            if (f.exists()) return f
        }
        val asset = book.assetPath ?: return null
        val dir = File(context.cacheDir, "khajabad-assets").apply { mkdirs() }
        val file = File(dir, "${book.id}.pdf")
        if (file.exists() && file.length() > 0L) return file
        return runCatching {
            context.assets.open(asset).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }.getOrNull()
    }
}
