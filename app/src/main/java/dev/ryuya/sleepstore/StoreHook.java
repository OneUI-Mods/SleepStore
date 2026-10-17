package dev.ryuya.sleepstore;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Process;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

/** Applied only within the framework-selected scope. Never writes PM or MARs state. */
public final class StoreHook extends XposedModule {
    private static final ThreadLocal<Boolean> RAW = ThreadLocal.withInitial(() -> false);
    private static final Set<String> READS = Set.of("getInstalledPackages", "getInstalledPackagesAsUser",
        "getInstalledApplications", "getInstalledApplicationsAsUser", "getPackageInfo", "getPackageInfoAsUser",
        "getApplicationInfo", "getApplicationInfoAsUser");
    private static final Set<String> EXCLUDED = Set.of("android", "com.android.systemui",
        "com.samsung.android.lool", "com.samsung.android.sm", "com.samsung.android.sm_cn");

    private final List<ModernHook.Handle> hooks = new ArrayList<>();
    private final Set<Method> hooked = new HashSet<>();
    private Object remote;
    private Method rawState;
    private Method rawAuto;
    private String process;

    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        ModernHook.attach(this);
        process = param.getProcessName();
        ModernHook.log("MODULE_LOADED api=" + ModernHook.apiVersion()
            + " framework=" + ModernHook.frameworkDescription() + " process=" + process);
    }

    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!param.isFirstPackage()) return;
        String pkg = param.getPackageName();
        if (BuildConfig.APPLICATION_ID.equals(pkg)) {
            installFrameworkIndicator(param.getClassLoader());
            return;
        }
        // Vector / LSPosed scope is the allowlist. Never change policy owners' view.
        if (EXCLUDED.contains(pkg) || "system_server".equals(process)) return;
        // Samsung's private API is verified for this Android major version only.
        if (Build.VERSION.SDK_INT != 36 || !"samsung".equalsIgnoreCase(Build.MANUFACTURER)) {
            ModernHook.log("UNSUPPORTED package=" + pkg + " sdk=" + Build.VERSION.SDK_INT
                + " manufacturer=" + Build.MANUFACTURER);
            return;
        }
        try {
            Class<?> globals = ModernHook.findClass("android.app.AppGlobals", null);
            remote = invoke(globals.getDeclaredMethod("getPackageManager"), null);
            rawState = remote.getClass().getMethod("getApplicationEnabledSetting", String.class, int.class);
            rawAuto = remote.getClass().getMethod("isPackageAutoDisabled", String.class, int.class);
            install(ModernHook.findClass("android.app.ApplicationPackageManager", null));
            install(remote.getClass());
            ModernHook.log("HOOK_READY package=" + pkg + " process=" + process + " methods=" + hooked.size());
            hooks.add(ModernHook.findAndHookMethod(Application.class, "attach", Context.class,
                new ModernHook.Callback() {
                    @Override protected void afterHookedMethod(ModernHook.Param p) {
                        if (p.hasThrowable()) return;
                        Context context = (Context)p.args[0];
                        new Thread(() -> diagnose(context), "SleepStore-check").start();
                    }
                }));
        } catch (Throwable e) {
            // A changed or inaccessible Samsung contract leaves the host untouched.
            ModernHook.log("HOST_DISABLED package=" + pkg + " reason=" + e);
            unhookAll();
        }
    }

    private void installFrameworkIndicator(ClassLoader loader) {
        try {
            Class<?> activity = ModernHook.findClass(MainActivity.class.getName(), loader);
            hooks.add(ModernHook.findAndHookMethod(activity, "frameworkLoaded",
                new ModernHook.Callback() {
                    @Override protected void beforeHookedMethod(ModernHook.Param p) {
                        p.setResult(Boolean.TRUE);
                    }
                }));
            ModernHook.log("UI_INDICATOR_READY");
        } catch (Throwable e) {
            ModernHook.log("UI_INDICATOR_DISABLED " + e);
        }
    }

    private void install(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            if (!READS.contains(name) && !name.equals("setApplicationEnabledSetting")
                && !name.equals("getApplicationEnabledSetting") && !name.equals("isPackageAutoDisabled")) continue;
            if (!hooked.add(method)) continue;
            method.setAccessible(true);
            if (READS.contains(name)) {
                hooks.add(ModernHook.hookMethod(method, new ModernHook.Callback() {
                    @Override protected void beforeHookedMethod(ModernHook.Param p) throws Throwable {
                        if (RAW.get()) return;
                        int index = name.startsWith("getInstalled") ? 0 : 1;
                        if (index >= p.args.length) return;
                        Object flags = p.args[index];
                        if (flags instanceof Integer) p.args[index] = (Integer)flags | 0x8000;
                        else if (flags instanceof Long) p.args[index] = (Long)flags | 0x8000L;
                        else if (flags != null && flags.getClass().getName().startsWith("android.content.pm.PackageManager$")) {
                            long value = ((Number)call(flags, "getValue")).longValue();
                            p.args[index] = callStatic(flags.getClass(), "of", new Class<?>[]{long.class}, value | 0x8000L);
                        }
                    }
                    @Override protected void afterHookedMethod(ModernHook.Param p) {
                        if (RAW.get() || p.hasThrowable()) return;
                        try { p.setResult(adapt(p.getResult())); }
                        catch (Throwable e) { ModernHook.log("READ_UNCHANGED method=" + name + " reason=" + e); }
                    }
                }));
            } else if (name.equals("setApplicationEnabledSetting")) {
                hooks.add(ModernHook.hookMethod(method, new ModernHook.Callback() {
                    @Override protected void beforeHookedMethod(ModernHook.Param p) {
                        if (RAW.get() || p.args.length < 3 || !(p.args[0] instanceof String)) return;
                        int requested = (Integer)p.args[1];
                        int user = p.args.length >= 5 && p.args[3] instanceof Integer
                            ? (Integer)p.args[3] : Process.myUid() / 100000;
                        if ((requested == 0 || requested == 1) && deep((String)p.args[0], user)) {
                            p.setResult(null);
                            ModernHook.log("ENABLE_BLOCKED package=" + p.args[0] + " process=" + process);
                        }
                    }
                }));
            } else {
                hooks.add(ModernHook.hookMethod(method, new ModernHook.Callback() {
                    @Override protected void afterHookedMethod(ModernHook.Param p) {
                        if (RAW.get() || p.hasThrowable() || p.args.length == 0 || !(p.args[0] instanceof String)) return;
                        int user = p.args.length > 1 && p.args[1] instanceof Integer
                            ? (Integer)p.args[1] : Process.myUid() / 100000;
                        if (deep((String)p.args[0], user))
                            p.setResult(name.equals("isPackageAutoDisabled") ? Boolean.FALSE : Integer.valueOf(0));
                    }
                }));
            }
        }
    }

    private Object raw(Method method, String pkg, int user) throws Exception {
        boolean previous = RAW.get(); RAW.set(true);
        try { return invoke(method, remote, pkg, user); }
        finally { RAW.set(previous); }
    }

    private boolean deep(String pkg, int user) {
        try { return Eligibility.isDeepSleeping((Integer)raw(rawState, pkg, user), (Boolean)raw(rawAuto, pkg, user)); }
        catch (Throwable ignored) { return false; }
    }

    private ApplicationInfo app(ApplicationInfo info) {
        if (info == null || (info.enabled && setting(info) != 4)
            || !deep(info.packageName, info.uid / 100000)) return info;
        ApplicationInfo copy = new ApplicationInfo(info);
        copy.enabled = true;
        setIntField(copy, "enabledSetting", 0);
        return copy;
    }

    private static int setting(ApplicationInfo info) {
        try { return getField(info.getClass(), "enabledSetting").getInt(info); }
        catch (Throwable ignored) { return -1; }
    }

    private Object adapt(Object value) throws Throwable {
        if (value instanceof ApplicationInfo) return app((ApplicationInfo)value);
        if (value instanceof PackageInfo) {
            PackageInfo old = (PackageInfo)value;
            ApplicationInfo ai = app(old.applicationInfo);
            if (ai == old.applicationInfo) return old;
            Parcel parcel = Parcel.obtain();
            try {
                old.writeToParcel(parcel, 0); parcel.setDataPosition(0);
                PackageInfo copy = PackageInfo.CREATOR.createFromParcel(parcel);
                copy.applicationInfo = ai;
                return copy;
            } finally { parcel.recycle(); }
        }
        if (value instanceof List<?>) {
            List<Object> copy = new ArrayList<>();
            boolean changed = false;
            for (Object item : (List<?>)value) {
                Object next = adapt(item); copy.add(next); changed |= next != item;
            }
            return changed ? copy : value;
        }
        if (value != null && value.getClass().getName().equals("android.content.pm.ParceledListSlice")) {
            Object list = call(value, "getList");
            Object next = adapt(list);
            if (next != list) {
                Constructor<?> constructor = value.getClass().getDeclaredConstructor(List.class);
                constructor.setAccessible(true);
                return constructor.newInstance(next);
            }
        }
        return value;
    }

    /** Read-only checks through exactly the PackageManager API used by stores. */
    private void diagnose(Context context) {
        try {
            int deepCount = 0, preserved = 0, failed = 0;
            boolean previous = RAW.get(); RAW.set(true);
            List<ApplicationInfo> original;
            try { original = context.getPackageManager().getInstalledApplications(0x8200); }
            finally { RAW.set(previous); }
            for (ApplicationInfo ai : original) {
                int user = ai.uid / 100000;
                int state = (Integer)raw(rawState, ai.packageName, user);
                boolean auto = (Boolean)raw(rawAuto, ai.packageName, user);
                int visible = context.getPackageManager().getApplicationEnabledSetting(ai.packageName);
                ApplicationInfo read = context.getPackageManager().getApplicationInfo(ai.packageName, 0x8200);
                if (Eligibility.isDeepSleeping(state, auto)) {
                    deepCount++;
                    if (visible != 0 || !read.enabled) failed++;
                } else if (state == 2 || state == 3 || state == 4) {
                    preserved++;
                    if (visible != state || read.enabled != ai.enabled) failed++;
                }
            }
            JSONObject result = new JSONObject().put("process", process).put("deepSleepingVisible", deepCount)
                .put("disabledPreserved", preserved).put("failures", failed).put("api", ModernHook.apiVersion())
                .put("time", System.currentTimeMillis());
            try (java.io.FileOutputStream out = context.openFileOutput("sleepstore-diagnostics.json", Context.MODE_PRIVATE)) {
                out.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            ModernHook.log("CHECK " + result);
        } catch (Throwable e) { ModernHook.log("CHECK_ERROR " + e); }
    }

    private void unhookAll() {
        for (ModernHook.Handle hook : hooks) {
            try { hook.unhook(); } catch (Throwable ignored) { }
        }
        hooks.clear(); hooked.clear();
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws Exception {
        method.setAccessible(true);
        try { return method.invoke(receiver, args); }
        catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception)cause;
            if (cause instanceof Error) throw (Error)cause;
            throw e;
        }
    }

    private static Object call(Object receiver, String name) throws Exception {
        return invoke(receiver.getClass().getMethod(name), receiver);
    }

    private static Object callStatic(Class<?> owner, String name, Class<?>[] types, Object... args) throws Exception {
        return invoke(owner.getMethod(name, types), null, args);
    }

    private static Field getField(Class<?> owner, String name) throws NoSuchFieldException {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    private static void setIntField(Object receiver, String name, int value) {
        try { getField(receiver.getClass(), name).setInt(receiver, value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
