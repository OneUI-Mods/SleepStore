package dev.ryuya.sleepstore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Xml
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

internal object DeepSleepRepair {
    val supported: Boolean
        get() = Build.VERSION.SDK_INT == 37 && Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    fun run(context: Context, packages: List<String>? = null): JSONObject {
        check(supported)
        val user = Process.myUid() / 100000
        val apk = context.applicationInfo.sourceDir
        val mode = if (packages == null) "scan" else "repair"
        val args = listOf(mode, user.toString()) + packages.orEmpty()
        check(packages.orEmpty().all(DeepSleepRepairPolicy.packageName::matches))
        val command = "CLASSPATH=${quote(apk)} exec /system/bin/timeout -k 2 28 " +
            "/system/bin/app_process /system/bin ${DeepSleepRepairMain::class.java.name} ${args.joinToString(" ")}"
        val result = ProcessBuilder("/system/bin/timeout", "-k", "2", "35", "su", "-c", command)
            .redirectErrorStream(true).start()
        try {
            result.outputStream.close()
            val output = result.inputStream.bufferedReader().use { it.readLines() }
            val line = output.lastOrNull { it.startsWith("SLEEPSTORE_RESULT ") }
            if (result.waitFor() != 0 || line == null) throw IOException("Root repair unavailable")
            return JSONObject(line.removePrefix("SLEEPSTORE_RESULT "))
        } finally {
            result.destroy()
        }
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}

internal object DeepSleepRepairMain {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            check(Process.myUid() == 0 && DeepSleepRepair.supported)
            val mode = args[0]
            val user = args[1].toInt()
            check(user >= 0 && mode in setOf("scan", "repair"))
            val confirmed = args.drop(2).toSet()
            check(confirmed.all(DeepSleepRepairPolicy.packageName::matches))

            val candidates = scan(user)
            val pm = RepairPackageManager(user)
            val ready = candidates.filter { (name, uid) -> pm.canRepair(name, uid) }
            val repaired = mutableListOf<String>()
            val failed = mutableListOf<String>()
            if (mode == "repair") {
                for ((name, uid) in ready) {
                    if (name !in confirmed) continue
                    if (runCatching { pm.restore(name, uid) }.getOrDefault(false)) repaired.add(name)
                    else failed.add(name)
                }
            }
            val result = JSONObject()
                .put("packages", JSONArray(ready.keys.sorted()))
                .put("repaired", JSONArray(repaired))
                .put("failed", JSONArray(failed))
                .put("skipped", if (mode == "repair") (confirmed - ready.keys).size else 0)
            println("SLEEPSTORE_RESULT $result")
        } catch (error: Exception) {
            System.err.println("SleepStore repair unavailable: ${error.javaClass.simpleName}")
            kotlin.system.exitProcess(1)
        }
    }

    private fun scan(user: Int): Map<String, Int> {
        val mars = readCommand("/system/bin/dumpsys", "-t", "6", "activity", "mars") { it.readText() }
        check("-curLv " in mars && "-maxLv " in mars)
        val asleep = DeepSleepRepairPolicy.sleepingPackages(mars, user)
        if (asleep.isEmpty()) return emptyMap()

        return readCommand("/system/bin/abx2xml", "/data/system/users/$user/package-restrictions.xml", "-") { reader ->
            val xml = Xml.newPullParser().apply { setInput(reader) }
            val candidates = mutableMapOf<String, Int>()
            while (xml.next() != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType != XmlPullParser.START_TAG || xml.depth != 2 || xml.name != "pkg") continue
                val name = xml.getAttributeValue(null, "name") ?: continue
                val uid = asleep[name] ?: continue
                if (
                    DeepSleepRepairPolicy.wasResetByStore(
                        xml.getAttributeValue(null, "enabled"),
                        xml.getAttributeValue(null, "enabledCaller"),
                        xml.getAttributeValue(null, "stopped"),
                    )
                ) candidates[name] = uid
            }
            candidates
        }
    }

    private fun <T> readCommand(vararg command: String, read: (java.io.Reader) -> T): T {
        val process = ProcessBuilder(*command).redirectError(java.io.File("/dev/null")).start()
        try {
            val result = process.inputStream.bufferedReader().use(read)
            check(process.waitFor() == 0)
            return result
        } finally {
            process.destroy()
        }
    }

    private class RepairPackageManager(private val user: Int) {
        private val contract = Class.forName("android.content.pm.IPackageManager")
        private val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, "package") as IBinder
        private val remote = Class.forName("android.content.pm.IPackageManager\$Stub")
            .getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        private val intType = Int::class.javaPrimitiveType
        private val enabled = contract.getMethod("getApplicationEnabledSetting", String::class.java, intType)
        private val info = contract.getMethod(
            "getApplicationInfo", String::class.java, Long::class.javaPrimitiveType, intType,
        )
        private val set = contract.getMethod(
            "setApplicationEnabledSetting", String::class.java, intType, intType, intType, String::class.java,
        )
        private val autoDisabled = contract.getMethod("isPackageAutoDisabled", String::class.java, intType)

        fun canRepair(name: String, uid: Int): Boolean {
            val app = info.invoke(remote, name, PackageManager.MATCH_DISABLED_COMPONENTS.toLong(), user)
                as? ApplicationInfo ?: return false
            return app.uid == uid &&
                (app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0 &&
                (app.flags and ApplicationInfo.FLAG_STOPPED) != 0 &&
                enabled.invoke(remote, name, user) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }

        fun restore(name: String, uid: Int): Boolean {
            if (!canRepair(name, uid)) return false
            // This is the attribution used by Samsung's PM to identify an automatic disable.
            set.invoke(remote, name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED, 0, user, "auto_disabler")
            return autoDisabled.invoke(remote, name, user) == true
        }
    }
}
