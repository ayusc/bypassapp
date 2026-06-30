package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge.log  // Required for logs in LSPosed UI!
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        try {
            log("[BypassUSB] 🚀 Booting hook for Uber Driver")

            findAndHookMethod(
                /* 1 */ "com.ubercab.force_app_upgrade.c",         // fully qualified class name
                /* 2 */ lpparam.classLoader,                       // ClassLoader of target app
                /* 3 */ "b",                                        // method name → b(Object):boolean
                /* 4 */ java.lang.Object::class.java               // parameter type = Object — DO NOT USE ::class.java IN MODERN KOTLIN!
            ) object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    log("[BypassUSB] 💥 Detected 'update required' check!")
                    param?.result = false  // Always skip forced update screen!
                    log("[BypassUSB] ✅ Bypass successful! You are now online.")
                }
            }

        } catch (t: Throwable) {
            t.printStackTrace()
            log("[BypassUSB] ❌ Hook failed: ${t.message}")
        }
    }
}
