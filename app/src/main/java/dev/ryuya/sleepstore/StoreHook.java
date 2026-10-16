package dev.ryuya.sleepstore;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Process;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Only installed in store processes. Never writes PackageManager or MARs state. */
public final class StoreHook implements IXposedHookLoadPackage {
    private static final ThreadLocal<Boolean> RAW = ThreadLocal.withInitial(() -> false);
    private static final Set<String> READS = Set.of("getInstalledPackages", "getInstalledPackagesAsUser",
        "getInstalledApplications", "getInstalledApplicationsAsUser", "getPackageInfo", "getPackageInfoAsUser",
        "getApplicationInfo", "getApplicationInfoAsUser");
    private static Object remote;
    private static Method rawState, rawAuto;
    private static String process;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (lp.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(MainActivity.class.getName(), lp.classLoader,
                "frameworkLoaded", XC_MethodReplacement.returnConstant(true));
            return;
        }
        if (!lp.packageName.equals("com.android.vending") && !lp.packageName.equals("com.sec.android.app.samsungapps")) return;
        // Samsung's private API is verified for this Android major version only.
        if (Build.VERSION.SDK_INT != 36 || !"samsung".equalsIgnoreCase(Build.MANUFACTURER)) return;
        process = lp.processName;
        Class<?> globals = XposedHelpers.findClass("android.app.AppGlobals", null);
        remote = XposedHelpers.callStaticMethod(globals, "getPackageManager");
        rawState = remote.getClass().getMethod("getApplicationEnabledSetting", String.class, int.class);
        rawAuto = remote.getClass().getMethod("isPackageAutoDisabled", String.class, int.class);
        Class<?> apm = XposedHelpers.findClass("android.app.ApplicationPackageManager", null);
        install(apm);
        install(remote.getClass());
        XposedBridge.log("SleepStore: installed " + process);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Context context = (Context)p.args[0];
                new Thread(() -> diagnose(context), "SleepStore-check").start();
            }
        });
    }

    private static void install(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            if (READS.contains(name)) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (RAW.get()) return;
                        // Include until-used packages in reads, without changing the real enabled state.
                        int index = name.startsWith("getInstalled") ? 0 : 1;
                        if (index >= p.args.length) return;
                        Object flags = p.args[index];
                        if (flags instanceof Integer) p.args[index] = (Integer)flags | 0x8000;
                        else if (flags instanceof Long) p.args[index] = (Long)flags | 0x8000L;
                        else if (flags != null && flags.getClass().getName().startsWith("android.content.pm.PackageManager$")) {
                            long value = ((Number)XposedHelpers.callMethod(flags, "getValue")).longValue();
                            p.args[index] = XposedHelpers.callStaticMethod(flags.getClass(), "of", value | 0x8000L);
                        }
                    }
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (RAW.get() || p.hasThrowable()) return;
                        try { p.setResult(adapt(p.getResult())); }
                        catch (Throwable e) { XposedBridge.log("SleepStore: read unchanged: " + e); }
                    }
                });
            } else if (name.equals("getApplicationEnabledSetting") || name.equals("isPackageAutoDisabled")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (RAW.get() || p.hasThrowable() || p.args.length == 0) return;
                        int user = p.args.length > 1 && p.args[1] instanceof Integer ? (Integer)p.args[1] : Process.myUid()/100000;
                        if (deep((String)p.args[0], user)) p.setResult(name.equals("isPackageAutoDisabled") ? Boolean.FALSE : Integer.valueOf(0));
                    }
                });
            }
        }
    }

    private static Object raw(Method method, String pkg, int user) throws Exception {
        boolean previous = RAW.get(); RAW.set(true);
        try { return method.invoke(remote, pkg, user); }
        finally { RAW.set(previous); }
    }
    private static boolean deep(String pkg, int user) {
        try { return Eligibility.isDeepSleeping((Integer)raw(rawState,pkg,user), (Boolean)raw(rawAuto,pkg,user)); }
        catch (Throwable ignored) { return false; } // Unknown or denied: preserve the original response.
    }
    private static ApplicationInfo app(ApplicationInfo info) {
        if (info == null || (info.enabled && setting(info) != 4) || !deep(info.packageName, info.uid/100000)) return info;
        ApplicationInfo copy = new ApplicationInfo(info);
        copy.enabled = true;
        XposedHelpers.setIntField(copy, "enabledSetting", 0);
        return copy;
    }
    private static int setting(ApplicationInfo info) {
        try { return XposedHelpers.getIntField(info, "enabledSetting"); }
        catch (Throwable ignored) { return -1; }
    }
    private static Object adapt(Object value) throws Throwable {
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
            for (Object item : (List<?>)value) { Object next=adapt(item); copy.add(next); changed |= next != item; }
            return changed ? copy : value;
        }
        if (value != null && value.getClass().getName().equals("android.content.pm.ParceledListSlice")) {
            Object list = XposedHelpers.callMethod(value, "getList");
            Object next = adapt(list);
            if (next != list) return XposedHelpers.newInstance(value.getClass(), next);
        }
        return value;
    }

    /** Read-only checks through exactly the PackageManager API used by stores. */
    private static void diagnose(Context context) {
        try {
            int deepCount=0, preserved=0, failed=0;
            boolean previous=RAW.get(); RAW.set(true);
            List<ApplicationInfo> original;
            try { original=context.getPackageManager().getInstalledApplications(0x8200); }
            finally { RAW.set(previous); }
            for (ApplicationInfo ai : original) {
                int user=ai.uid/100000;
                int state=(Integer)raw(rawState,ai.packageName,user);
                boolean auto=(Boolean)raw(rawAuto,ai.packageName,user);
                int visible=context.getPackageManager().getApplicationEnabledSetting(ai.packageName);
                ApplicationInfo read=context.getPackageManager().getApplicationInfo(ai.packageName,0x8200);
                if (Eligibility.isDeepSleeping(state,auto)) {
                    deepCount++;
                    if (visible!=0 || !read.enabled) failed++;
                } else if (state==2 || state==3 || state==4) {
                    preserved++;
                    if (visible!=state || read.enabled!=ai.enabled) failed++;
                }
            }
            JSONObject result=new JSONObject().put("process",process).put("deepSleepingVisible",deepCount)
                .put("disabledPreserved",preserved).put("failures",failed).put("time",System.currentTimeMillis());
            try(java.io.FileOutputStream out=context.openFileOutput("sleepstore-diagnostics.json",Context.MODE_PRIVATE)) {
                out.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            XposedBridge.log("SleepStore: CHECK " + result);
        } catch(Throwable e) { XposedBridge.log("SleepStore: CHECK error " + e); }
    }
}
