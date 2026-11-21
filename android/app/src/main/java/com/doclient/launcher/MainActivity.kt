package com.doclient.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

/**
 * DOClient Android launcher shell (v0.8).
 * Native tools: offline UUIDs, instance folders + options.txt presets, Mojang version list,
 * local content import, Modrinth search/install with SHA-512 verification.
 * NOT included yet: Java runtime, renderer, game launch bridge, Microsoft OAuth.
 */
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val io = Executors.newFixedThreadPool(3)

    private var pendingImageKind = "skin"
    private var pendingContentId = ""
    private var pendingContentKind = ""

    private val idRe = Regex("^[a-z0-9]{4,24}$")
    private val verRe = Regex("^[0-9A-Za-z._\\- ]{1,32}$")
    private val projRe = Regex("^[A-Za-z0-9]{6,12}$")
    private val nameRe = Regex("^[A-Za-z0-9_]{3,16}$")
    private val loaders = setOf("fabric", "forge", "neoforge", "quilt")
    private val kinds = setOf("mods", "resourcepacks", "shaderpacks")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 5, 26)
        window.navigationBarColor = Color.rgb(9, 5, 26)
        window.decorView.systemUiVisibility = 0

        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(9, 5, 26))
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true // bundled android_asset files only
            allowContentAccess = false // images are handed to the UI as data: URLs
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return true
                if (uri.scheme == "file" && uri.path?.startsWith("/android_asset/") == true) return false
                if (uri.scheme == "https" && uri.host == "discord.gg") openExternal(uri)
                return true
            }
        }
        webView.webChromeClient = WebChromeClient()
        setContentView(webView)
        webView.loadUrl("file:///android_asset/android.html")
    }

    // ---------- helpers ----------
    private fun gameRoot(): File = File(getExternalFilesDir(null) ?: filesDir, "instances")

    private fun instDir(id: String): File? = if (idRe.matches(id)) File(gameRoot(), id) else null

    private fun contentDir(id: String, kind: String): File? {
        if (kind !in kinds) return null
        val base = instDir(id) ?: return null
        return File(base, kind).also { it.mkdirs() }
    }

    private fun allowedExt(kind: String, name: String): Boolean {
        val n = name.lowercase()
        return if (kind == "mods") n.endsWith(".jar") else n.endsWith(".zip")
    }

    private fun safeFile(dir: File, name: String): File? {
        val clean = File(name).name
        if (clean.isBlank() || clean.startsWith(".")) return null
        val f = File(dir, clean)
        return if (f.canonicalPath.startsWith(dir.canonicalPath + File.separator)) f else null
    }

    private fun callJs(fn: String, vararg args: Any?) {
        val a = args.joinToString(",") {
            when (it) {
                null -> "null"
                is Boolean, is Number -> it.toString()
                else -> JSONObject.quote(it.toString())
            }
        }
        runOnUiThread {
            if (::webView.isInitialized) webView.evaluateJavascript("window.$fn&&window.$fn($a)", null)
        }
    }

    private fun openConn(url: String, hosts: Set<String>): HttpURLConnection {
        val u = URL(url)
        require(u.protocol == "https" && u.host in hosts) { "Blocked host" }
        return (u.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 25_000
            setRequestProperty("User-Agent", UA)
        }
    }

    private fun getText(url: String, hosts: Set<String>): String {
        val c = openConn(url, hosts)
        try {
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun optionsFor(preset: String): String {
        val p = when (preset) {
            "potato" -> listOf("renderDistance:4", "simulationDistance:5", "graphicsMode:0", "maxFps:120",
                "particles:2", "entityShadows:false", "ao:false", "biomeBlendRadius:0", "mipmapLevels:0",
                "entityDistanceScaling:0.5", "renderClouds:\"false\"", "bobView:false", "enableVsync:false")
            "quality" -> listOf("renderDistance:12", "simulationDistance:10", "graphicsMode:1", "maxFps:60",
                "particles:0", "entityShadows:true", "ao:true", "biomeBlendRadius:2", "mipmapLevels:4",
                "entityDistanceScaling:1.0", "enableVsync:true")
            else -> listOf("renderDistance:8", "simulationDistance:8", "graphicsMode:0", "maxFps:90",
                "particles:1", "entityShadows:false", "ao:true", "biomeBlendRadius:1", "mipmapLevels:2",
                "entityDistanceScaling:0.75", "enableVsync:false")
        }
        return (p + "fullscreen:false").joinToString("\n") + "\n"
    }

    // ---------- JS bridge ----------
    private inner class AndroidBridge {
        @JavascriptInterface
        fun openDiscord() {
            runOnUiThread { openExternal(Uri.parse("https://discord.gg/5Bc4a822M")) }
        }

        /** The Java runtime + renderer + launch bridge is not integrated yet. */
        @JavascriptInterface
        fun engineAvailable(): Boolean = false

        @JavascriptInterface
        fun offlineUuid(name: String): String {
            if (!nameRe.matches(name)) return ""
            return UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8)).toString()
        }

        @JavascriptInterface
        fun pickImage(kind: String) {
            runOnUiThread {
                pendingImageKind = if (kind == "cape") "cape" else "skin"
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/png"
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_IMAGE)
            }
        }

        @JavascriptInterface
        fun fetchVersions() {
            io.execute {
                try {
                    val cache = File(filesDir, "version_manifest.json")
                    var text: String? = null
                    if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 15 * 60 * 1000) {
                        text = cache.readText()
                    }
                    if (text == null) {
                        try {
                            text = getText(MANIFEST, setOf("launchermeta.mojang.com"))
                            cache.writeText(text)
                        } catch (e: Exception) {
                            if (cache.exists()) text = cache.readText() else throw e
                        }
                    }
                    val m = JSONObject(text!!)
                    val arr = m.getJSONArray("versions")
                    val out = JSONArray()
                    for (i in 0 until arr.length()) {
                        val v = arr.getJSONObject(i)
                        if (v.getString("type") == "release") out.put(v.getString("id"))
                    }
                    val res = JSONObject()
                        .put("latest", m.getJSONObject("latest").getString("release"))
                        .put("releases", out)
                    callJs("onVersions", true, res.toString())
                } catch (e: Exception) {
                    callJs("onVersions", false, e.message ?: "Network error")
                }
            }
        }

        @JavascriptInterface
        fun initInstance(id: String, preset: String): Boolean {
            val dir = instDir(id) ?: return false
            return try {
                dir.mkdirs()
                for (k in kinds) File(dir, k).mkdirs()
                val opt = File(dir, "options.txt")
                if (!opt.exists()) opt.writeText(optionsFor(preset))
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun deleteInstance(id: String): Boolean {
            val dir = instDir(id) ?: return false
            return try { dir.deleteRecursively() } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun pickContent(id: String, kind: String) {
            if (!idRe.matches(id) || kind !in kinds) return
            runOnUiThread {
                pendingContentId = id
                pendingContentKind = kind
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_CONTENT)
            }
        }

        @JavascriptInterface
        fun listContent(id: String, kind: String): String {
            val dir = contentDir(id, kind) ?: return "[]"
            val out = JSONArray()
            dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() }
                ?.forEach { out.put(JSONObject().put("name", it.name).put("size", it.length())) }
            return out.toString()
        }

        @JavascriptInterface
        fun deleteContent(id: String, kind: String, name: String): Boolean {
            val dir = contentDir(id, kind) ?: return false
            val f = safeFile(dir, name) ?: return false
            return f.isFile && f.delete()
        }

        @JavascriptInterface
        fun modrinthSearch(query: String, version: String, loader: String, kind: String, reqId: String) {
            io.execute {
                try {
                    if (kind !in kinds) throw IOException("Bad content type")
                    val type = when (kind) { "mods" -> "mod"; "resourcepacks" -> "resourcepack"; else -> "shader" }
                    val facets = JSONArray().put(JSONArray().put("project_type:$type"))
                    if (verRe.matches(version)) facets.put(JSONArray().put("versions:$version"))
                    if (kind == "mods" && loader in loaders) facets.put(JSONArray().put("categories:$loader"))
                    val q = query.trim().take(64)
                    val url = "https://api.modrinth.com/v2/search?query=${enc(q)}&limit=20" +
                        "&index=${if (q.isEmpty()) "downloads" else "relevance"}&facets=${enc(facets.toString())}"
                    val hits = JSONObject(getText(url, setOf("api.modrinth.com"))).getJSONArray("hits")
                    val out = JSONArray()
                    for (i in 0 until hits.length()) {
                        val h = hits.getJSONObject(i)
                        out.put(JSONObject()
                            .put("id", h.optString("project_id"))
                            .put("title", h.optString("title"))
                            .put("desc", h.optString("description"))
                            .put("icon", h.optString("icon_url"))
                            .put("downloads", h.optLong("downloads"))
                            .put("author", h.optString("author")))
                    }
                    callJs("onModrinthSearch", reqId, true, out.toString())
                } catch (e: Exception) {
                    callJs("onModrinthSearch", reqId, false, e.message ?: "Network error")
                }
            }
        }

        @JavascriptInterface
        fun modrinthInstall(id: String, kind: String, projectId: String, version: String, loader: String) {
            io.execute {
                var tmp: File? = null
                try {
                    val dir = contentDir(id, kind) ?: throw IOException("Bad instance")
                    if (!projRe.matches(projectId) || !verRe.matches(version)) throw IOException("Bad request")
                    var url = "https://api.modrinth.com/v2/project/$projectId/version?game_versions=" +
                        enc("[\"$version\"]")
                    if (kind == "mods") {
                        if (loader !in loaders) throw IOException("Pick Fabric, Forge, NeoForge or Quilt first")
                        url += "&loaders=" + enc("[\"$loader\"]")
                    }
                    val versions = JSONArray(getText(url, setOf("api.modrinth.com")))
                    if (versions.length() == 0) throw IOException("No file for Minecraft $version")
                    val v = versions.getJSONObject(0)
                    val files = v.getJSONArray("files")
                    var file = files.getJSONObject(0)
                    for (i in 0 until files.length()) {
                        if (files.getJSONObject(i).optBoolean("primary")) { file = files.getJSONObject(i); break }
                    }
                    val fileUrl = file.getString("url")
                    val dest = safeFile(dir, file.getString("filename")) ?: throw IOException("Unsafe file name")
                    if (!allowedExt(kind, dest.name)) throw IOException("Unexpected file type")
                    val expected = file.optJSONObject("hashes")?.optString("sha512") ?: ""

                    val c = openConn(fileUrl, setOf("cdn.modrinth.com"))
                    val md = MessageDigest.getInstance("SHA-512")
                    tmp = File(dir, ".dl-${System.nanoTime()}.part")
                    try {
                        if (c.responseCode != 200) throw IOException("Download failed (HTTP ${c.responseCode})")
                        c.inputStream.use { inp ->
                            tmp.outputStream().use { out ->
                                val buf = ByteArray(32 * 1024)
                                var total = 0L
                                while (true) {
                                    val n = inp.read(buf)
                                    if (n < 0) break
                                    total += n
                                    if (total > MAX_DOWNLOAD) throw IOException("File too large")
                                    md.update(buf, 0, n)
                                    out.write(buf, 0, n)
                                }
                            }
                        }
                    } finally {
                        c.disconnect()
                    }
                    val hex = md.digest().joinToString("") { "%02x".format(it) }
                    if (expected.isNotEmpty() && !hex.equals(expected, ignoreCase = true)) {
                        throw IOException("Checksum mismatch - file discarded")
                    }
                    if (dest.exists()) dest.delete()
                    if (!tmp.renameTo(dest)) throw IOException("Could not save file")
                    tmp = null

                    var required = 0
                    val deps = v.optJSONArray("dependencies")
                    if (deps != null) for (i in 0 until deps.length()) {
                        if (deps.getJSONObject(i).optString("dependency_type") == "required") required++
                    }
                    val note = if (required > 0) " Needs $required other mod(s), e.g. Fabric API - install them too." else ""
                    callJs("onModrinthInstall", projectId, true, "Installed ${dest.name}.$note")
                } catch (e: Exception) {
                    callJs("onModrinthInstall", projectId, false, e.message ?: "Install failed")
                } finally {
                    tmp?.delete()
                }
            }
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "No app available to open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readLimited(inp: java.io.InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < max) {
            val n = inp.read(buf, 0, minOf(buf.size, max - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0) ?: ""
        }
        return uri.lastPathSegment ?: ""
    }

    @Deprecated("Legacy callback retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQUEST_IMAGE -> handleImage(data.data ?: return, pendingImageKind)
            REQUEST_CONTENT -> {
                val uris = ArrayList<Uri>()
                data.data?.let { uris.add(it) }
                data.clipData?.let { cd -> for (i in 0 until cd.itemCount) uris.add(cd.getItemAt(i).uri) }
                handleContent(uris, pendingContentId, pendingContentKind)
            }
        }
    }

    private fun handleImage(uri: Uri, kind: String) {
        io.execute {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { readLimited(it, MAX_IMAGE + 1) }
                    ?: throw IOException("Cannot read file")
                if (bytes.size > MAX_IMAGE) throw IOException("Image is larger than 1 MB")
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                if (o.outMimeType != "image/png") throw IOException("Not a PNG image")
                val w = o.outWidth
                val h = o.outHeight
                val okSkin = kind == "skin" && w % 64 == 0 && w <= 1024 && (h == w || h * 2 == w)
                val okCape = kind == "cape" && w % 64 == 0 && w <= 1024 && h * 2 == w
                if (!okSkin && !okCape) {
                    throw IOException(if (kind == "skin") "Skin must be 64x64 or 64x32" else "Cape must be 64x32")
                }
                val url = "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                callJs("onNativeImagePicked", kind, url, "")
            } catch (e: Exception) {
                callJs("onNativeImagePicked", kind, "", e.message ?: "Could not load image")
            }
        }
    }

    private fun handleContent(uris: List<Uri>, id: String, kind: String) {
        io.execute {
            var ok = 0
            var skipped = 0
            val dir = contentDir(id, kind)
            if (dir != null) for (u in uris) {
                try {
                    val dest = safeFile(dir, displayName(u))
                    if (dest == null || !allowedExt(kind, dest.name)) { skipped++; continue }
                    var total = 0L
                    contentResolver.openInputStream(u)?.use { inp ->
                        dest.outputStream().use { out ->
                            val buf = ByteArray(32 * 1024)
                            while (true) {
                                val n = inp.read(buf)
                                if (n < 0) break
                                total += n
                                if (total > MAX_DOWNLOAD) throw IOException("too large")
                                out.write(buf, 0, n)
                            }
                        }
                    } ?: throw IOException("unreadable")
                    ok++
                } catch (e: Exception) {
                    skipped++
                }
            } else skipped = uris.size
            callJs("onContentImported", kind, ok, skipped)
        }
    }

    override fun onBackPressed() {
        webView.evaluateJavascript("(function(){return window.onBack?window.onBack():false})()") { r ->
            if (r != "true") finish()
        }
    }

    override fun onDestroy() {
        io.shutdown()
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AndroidBridge")
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_IMAGE = 4107
        private const val REQUEST_CONTENT = 4108
        private const val MAX_IMAGE = 1_048_576
        private const val MAX_DOWNLOAD = 200L * 1024 * 1024
        private const val MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        private const val UA = "DOClient-Mobile/0.8 (https://discord.gg/5Bc4a822M)"
    }
}
