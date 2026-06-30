package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge.log // ← THIS GIVES YOU LOGS IN LSP MANAGER!
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "[BypassUSB]"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // === STEP 1: Verify we're in the right app ===
            if (lpparam.packageName != "com.ubercab.driver") {
                log("$TAG Ignoring package: ${lpparam.packageName}")
                return
            }

            log("$TAG 🛫 Hooking Uber Driver v${getAppVersion(lpparam)}")

            if (lpparam.processName != "com.ubercab.driver") {
                log("$TAG 🔀 Skipping secondary process: ${lpparam.processName}")
                return
            }

            // === STEP 2: Find and hook target method ===
            findAndHookMethod(
                "com.ubercab.force_app_upgrade.c",   // class name from smali_classes20/
                lpparam.classLoader,
                "b",                                // method name → b(Object):boolean
                java.lang.Object::class.java       // param type → DriverCheckIssue casted as Object
            ) { param: MethodHookParam ->
                log("$TAG ⚠️ Force upgrade check TRIGGERED!")
                log("$TAG 💥 OVERRIDING return value from ${param.result} → FALSE")
                
                param.result = false
                
                log("$TAG ✅ Successfully bypassed 'Update Required' screen.")
            }

        } catch (t: Throwable) {
            log("$TAG ❌ CRITICAL FAILURE! Hook failed with error:")
            t.printStackTrace()
        }
    }

    private fun getAppVersion(lpparam: XC_LoadPackage.LoadPackageParam): String {
        return try {
            val context = lpparam.classLoader.loadClass("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null)
            
            (context as? android.content.Context)
                ?.packageManager?.getPackageInfo(lpparam.packageName, 0)
                ?.versionName ?: "unknown"
        } catch (e: Throwable) {
            e.printStackTrace()
            "unknown"
        }
    }
}
