package dev.ryuya.sleepstore

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Parcel
import android.os.Process
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.Boolean as JavaBoolean
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashSet
import kotlin.jvm.JvmStatic
import kotlin.math.log
import org.json.JSONObject

/* Applied only within the framework-selected scope. Never writes PM or MARs state. */
class StoreHook : XposedModule() {
    private val hooks: MutableList<ModernHook.Handle?> = ArrayList<ModernHook.Handle?>()
    private val hooked: MutableSet<Method?> = HashSet<Method?>()
    private var remote: Any? = null
    private var rawState: Method? = null
    private var rawAuto: Method? = null
    private var process: String? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        ModernHook.attach(this)
        process = param.getProcessName()
        ModernHook.log(
            ("MODULE_LOADED api=" +
                ModernHook.apiVersion() +
                " framework=" +
                ModernHook.frameworkDescription() +
                " process=" +
                process)
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage()) return
        val pkg = param.getPackageName()
        if (BuildConfig.APPLICATION_ID.equals(pkg)) {
            installFrameworkIndicator(param.getClassLoader())
            return
        }
        // Vector / LSPosed scope is the allowlist. Never change policy owners' view.
        if (StoreHook.EXCLUDED!!.contains(pkg) || "system_server" == process) return
        // Samsung's private API is verified for this Android major version only.
        if (
            Build.VERSION.SDK_INT != 36 || !"samsung".equals(Build.MANUFACTURER, ignoreCase = true)
        ) {
            ModernHook.log(
                ("UNSUPPORTED package=" +
                    pkg +
                    " sdk=" +
                    Build.VERSION.SDK_INT +
                    " manufacturer=" +
                    Build.MANUFACTURER)
            )
            return
        }
        try {
            val globals = ModernHook.findClass("android.app.AppGlobals", null)
            remote = StoreHook.invoke(globals!!.getDeclaredMethod("getPackageManager"), null)
            rawState =
                remote!!
                    .javaClass
                    .getMethod(
                        "getApplicationEnabledSetting",
                        String::class.java,
                        Int::class.javaPrimitiveType,
                    )
            rawAuto =
                remote!!
                    .javaClass
                    .getMethod(
                        "isPackageAutoDisabled",
                        String::class.java,
                        Int::class.javaPrimitiveType,
                    )
            install(ModernHook.findClass("android.app.ApplicationPackageManager", null))
            install(remote!!.javaClass)
            ModernHook.log(
                "HOOK_READY package=" + pkg + " process=" + process + " methods=" + hooked.size
            )
            hooks.add(
                ModernHook.findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    object : ModernHook.Callback() {
                        override fun afterHookedMethod(p: ModernHook.Param) {
                            if (p.hasThrowable()) return
                            val context: Context? = p.args[0] as Context?
                            Thread(Runnable { diagnose(context!!) }, "SleepStore-check").start()
                        }
                    },
                )
            )
        } catch (e: Throwable) {
            // A changed or inaccessible Samsung contract leaves the host untouched.
            ModernHook.log("HOST_DISABLED package=" + pkg + " reason=" + e)
            unhookAll()
        }
    }

    private fun installFrameworkIndicator(loader: ClassLoader?) {
        try {
            val activity = ModernHook.findClass(MainActivity::class.java.getName(), loader)
            hooks.add(
                ModernHook.findAndHookMethod(
                    activity,
                    "frameworkLoaded",
                    object : ModernHook.Callback() {
                        override fun beforeHookedMethod(p: ModernHook.Param) {
                            p.setResult(JavaBoolean.TRUE)
                        }
                    },
                )
            )
            ModernHook.log("UI_INDICATOR_READY")
        } catch (e: Throwable) {
            ModernHook.log("UI_INDICATOR_DISABLED " + e)
        }
    }

    private fun install(type: Class<*>) {
        for (method in type.getDeclaredMethods()) {
            val name = method.getName()
            if (
                !StoreHook.READS!!.contains(name) &&
                    (name != "setApplicationEnabledSetting") &&
                    (name != "getApplicationEnabledSetting") &&
                    (name != "isPackageAutoDisabled")
            )
                continue
            if (!hooked.add(method)) continue
            method.setAccessible(true)
            if (StoreHook.READS!!.contains(name)) {
                hooks.add(
                    ModernHook.hookMethod(
                        method,
                        object : ModernHook.Callback() {
                            @Throws(Throwable::class)
                            override fun beforeHookedMethod(p: ModernHook.Param) {
                                if (StoreHook.RAW.get()) return
                                val index: Int = if (name.startsWith("getInstalled")) 0 else 1
                                if (index >= p.args.size) return
                                val flags: Any? = p.args[index]
                                if (flags is Int) p.args[index] = flags as Int or 0x8000
                                else if (flags is Long) p.args[index] = flags as Long or 0x8000L
                                else if (
                                    flags != null &&
                                        flags.javaClass
                                            .getName()
                                            .startsWith("android.content.pm.PackageManager$")
                                ) {
                                    val value =
                                        (StoreHook.call(flags, "getValue") as Number).toLong()
                                    p.args[index] =
                                        StoreHook.callStatic(
                                            flags.javaClass,
                                            "of",
                                            arrayOf<Class<*>?>(Long::class.javaPrimitiveType),
                                            value or 0x8000L,
                                        )
                                }
                            }

                            override fun afterHookedMethod(p: ModernHook.Param) {
                                if (StoreHook.RAW.get() || p.hasThrowable()) return
                                try {
                                    p.setResult(adapt(p.getResult()))
                                } catch (e: Throwable) {
                                    ModernHook.log("READ_UNCHANGED method=" + name + " reason=" + e)
                                }
                            }
                        },
                    )
                )
            } else if (name == "setApplicationEnabledSetting") {
                hooks.add(
                    ModernHook.hookMethod(
                        method,
                        object : ModernHook.Callback() {
                            override fun beforeHookedMethod(p: ModernHook.Param) {
                                if (
                                    StoreHook.RAW.get() || p.args.size < 3 || (p.args[0] !is String)
                                )
                                    return
                                val requested: Int = p.args[1] as Int
                                val user: Int =
                                    if (p.args.size >= 5 && p.args[3] is Int) p.args[3] as Int
                                    else Process.myUid() / 100000
                                if (
                                    (requested == 0 || requested == 1) &&
                                        deep(p.args[0] as String?, user)
                                ) {
                                    p.setResult(null)
                                    ModernHook.log(
                                        "ENABLE_BLOCKED package=" +
                                            p.args[0] +
                                            " process=" +
                                            process
                                    )
                                }
                            }
                        },
                    )
                )
            } else {
                hooks.add(
                    ModernHook.hookMethod(
                        method,
                        object : ModernHook.Callback() {
                            override fun afterHookedMethod(p: ModernHook.Param) {
                                if (
                                    StoreHook.RAW.get() ||
                                        p.hasThrowable() ||
                                        p.args.size == 0 ||
                                        (p.args[0] !is String)
                                )
                                    return
                                val user: Int =
                                    if (p.args.size > 1 && p.args[1] is Int) p.args[1] as Int
                                    else Process.myUid() / 100000
                                if (deep(p.args[0] as String?, user))
                                    p.setResult(
                                        if (name == "isPackageAutoDisabled") JavaBoolean.FALSE
                                        else 0
                                    )
                            }
                        },
                    )
                )
            }
        }
    }

    @Throws(Exception::class)
    private fun raw(method: Method, pkg: String?, user: Int): Any? {
        val previous = StoreHook.RAW.get()
        StoreHook.RAW.set(true)
        try {
            return StoreHook.invoke(method, remote, pkg, user)
        } finally {
            StoreHook.RAW.set(previous)
        }
    }

    private fun deep(pkg: String?, user: Int): Boolean {
        try {
            return Eligibility.isDeepSleeping(
                raw(rawState!!, pkg, user) as Int,
                raw(rawAuto!!, pkg, user) as Boolean,
            )
        } catch (ignored: Throwable) {
            return false
        }
    }

    private fun app(info: ApplicationInfo?): ApplicationInfo? {
        if (
            info == null ||
                (info.enabled && StoreHook.setting(info) != 4) ||
                !deep(info.packageName, info.uid / 100000)
        )
            return info
        val copy = ApplicationInfo(info)
        copy.enabled = true
        StoreHook.setIntField(copy, "enabledSetting", 0)
        return copy
    }

    @Throws(Throwable::class)
    private fun adapt(value: Any?): Any? {
        if (value is ApplicationInfo) return app(value as ApplicationInfo?)
        if (value is PackageInfo) {
            val old: PackageInfo? = value as PackageInfo?
            val ai = app(old!!.applicationInfo)
            if (ai === old!!.applicationInfo) return old
            val parcel = Parcel.obtain()
            try {
                old!!.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                val copy = PackageInfo.CREATOR.createFromParcel(parcel)
                copy.applicationInfo = ai
                return copy
            } finally {
                parcel.recycle()
            }
        }
        if (value is MutableList<*>) {
            val copy = ArrayList<Any?>()
            var changed: Boolean = false
            for (item in value as MutableList<*>) {
                val next = adapt(item)
                copy.add(next)
                changed = changed or (next !== item)
            }
            return if (changed) copy else value
        }
        if (value != null && value.javaClass.getName() == "android.content.pm.ParceledListSlice") {
            val list = StoreHook.call(value, "getList")
            val next = adapt(list)
            if (next !== list) {
                val constructor = value.javaClass.getDeclaredConstructor(MutableList::class.java)
                constructor.setAccessible(true)
                return constructor.newInstance(next)
            }
        }
        return value
    }

    /* Read-only checks through exactly the PackageManager API used by stores. */
    private fun diagnose(context: Context) {
        try {
            var deepCount: Int = 0
            var preserved: Int = 0
            var failed: Int = 0
            val previous = StoreHook.RAW.get()
            StoreHook.RAW.set(true)
            var original: MutableList<ApplicationInfo?>?
            try {
                original = context.getPackageManager().getInstalledApplications(0x8200)
            } finally {
                StoreHook.RAW.set(previous)
            }
            for (ai in original) {
                val user: Int = ai!!.uid / 100000
                val state: Int = raw(rawState!!, ai!!.packageName, user) as Int
                val auto: Boolean = raw(rawAuto!!, ai!!.packageName, user) as Boolean
                val visible =
                    context.getPackageManager().getApplicationEnabledSetting(ai!!.packageName)
                val read = context.getPackageManager().getApplicationInfo(ai!!.packageName, 0x8200)
                if (Eligibility.isDeepSleeping(state, auto)) {
                    deepCount++
                    if (visible != 0 || !read.enabled) failed++
                } else if (state == 2 || state == 3 || state == 4) {
                    preserved++
                    if (visible != state || read.enabled != ai!!.enabled) failed++
                }
            }
            val result =
                JSONObject()
                    .put("process", process)
                    .put("deepSleepingVisible", deepCount)
                    .put("disabledPreserved", preserved)
                    .put("failures", failed)
                    .put("api", ModernHook.apiVersion())
                    .put("time", System.currentTimeMillis())
            context.openFileOutput("sleepstore-diagnostics.json", Context.MODE_PRIVATE).use { out ->
                out.write(result.toString(2).toByteArray(StandardCharsets.UTF_8))
            }
            ModernHook.log("CHECK " + result)
        } catch (e: Throwable) {
            ModernHook.log("CHECK_ERROR " + e)
        }
    }

    private fun unhookAll() {
        for (hook in hooks) {
            try {
                hook!!.unhook()
            } catch (ignored: Throwable) {}
        }
        hooks.clear()
        hooked.clear()
    }

    companion object {
        private val RAW: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
        private val READS: MutableSet<String?>? =
            java.util.Set.of<String?>(
                "getInstalledPackages",
                "getInstalledPackagesAsUser",
                "getInstalledApplications",
                "getInstalledApplicationsAsUser",
                "getPackageInfo",
                "getPackageInfoAsUser",
                "getApplicationInfo",
                "getApplicationInfoAsUser",
            )
        private val EXCLUDED: MutableSet<String?>? =
            java.util.Set.of<String?>(
                "android",
                "com.android.systemui",
                "com.samsung.android.lool",
                "com.samsung.android.sm",
                "com.samsung.android.sm_cn",
            )

        @JvmStatic
        private fun setting(info: ApplicationInfo): Int {
            try {
                return StoreHook.getField(info.javaClass, "enabledSetting")!!.getInt(info)
            } catch (ignored: Throwable) {
                return -1
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun invoke(method: Method, receiver: Any?, vararg args: Any?): Any? {
            method.setAccessible(true)
            try {
                return method.invoke(receiver, *args)
            } catch (e: InvocationTargetException) {
                val cause = e.cause
                if (cause is Exception) throw cause as Exception
                if (cause is Error) throw cause as Error
                throw e
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun call(receiver: Any, name: String?): Any? {
            return StoreHook.invoke(receiver.javaClass.getMethod(name), receiver)
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun callStatic(
            owner: Class<*>,
            name: String?,
            types: Array<Class<*>?>,
            vararg args: Any?,
        ): Any? {
            return StoreHook.invoke(owner.getMethod(name, *types), null, *args)
        }

        @JvmStatic
        @Throws(NoSuchFieldException::class)
        private fun getField(owner: Class<*>, name: String?): Field? {
            var type: Class<*>? = owner
            while (type != null) {
                try {
                    val field = type.getDeclaredField(name)
                    field.setAccessible(true)
                    return field
                } catch (ignored: NoSuchFieldException) {}
                type = type.getSuperclass()
            }
            throw NoSuchFieldException(owner.getName() + "." + name)
        }

        @JvmStatic
        private fun setIntField(receiver: Any, name: String?, value: Int) {
            try {
                StoreHook.getField(receiver.javaClass, name)!!.setInt(receiver, value)
            } catch (e: ReflectiveOperationException) {
                throw IllegalStateException(e)
            }
        }
    }
}
