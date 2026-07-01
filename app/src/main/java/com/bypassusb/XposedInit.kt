package com.bypassusb

import android.content.pm.PackageInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    companion object {
        // Change this to match the exact package identifier of the targeted app
        private const val TARGET_PACKAGE = "com.lyft.android.driver"
        private const val TAG = "[WahBuddy-Spoofer]"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Filter out everything else so we only execute inside our target app process
        if (lpparam.packageName != TARGET_PACKAGE) return
        if (lpparam.processName != TARGET_PACKAGE) return

        log("$TAG Target application process loaded: ${lpparam.packageName}")

        try {
            // Hook the framework's PackageManager implementation responsible for resolving manifest attributes
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val requestedPackage = param.args[0] as? String

                        // Verify if the app is querying its own information
                        if (requestedPackage != null && requestedPackage == lpparam.packageName) {
                            val info = param.result as? PackageInfo
                            if (info != null) {
                                val originalCode = info.versionCode
                                
                                // Override the legacy int version code field
                                info.versionCode = 2000000000
                                
                                // Override the long version code field required for modern Android versions (Android 9+)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    info.setLongVersionCode(2000000000L)
                                }

                                // Apply the modified object back to the method results register
                                param.result = info
                                log("$TAG Intercepted getPackageInfo successful! Forced Code Change: $originalCode -> 2000000000")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("$TAG Critical initialization failure or hook signature error: ${t.message}")
        }
    }
}
