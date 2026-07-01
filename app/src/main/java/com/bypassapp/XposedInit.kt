package com.bypassapp

import android.content.pm.PackageInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PACKAGE = "com.lyft.android.driver"
        private const val TAG = "[WahBuddy-Spoofer]"
        
        // Legit values from the latest configuration
        private const val LATEST_VERSION_CODE = 1782286115
        private const val LATEST_VERSION_CODE_LONG = 1782286115L
        private const val LATEST_VERSION_NAME = "2026.24.3.1782286115"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return
        if (lpparam.processName != TARGET_PACKAGE) return

        log("$TAG Target application process loaded: ${lpparam.packageName}")

        try {
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

                        if (requestedPackage != null && requestedPackage == lpparam.packageName) {
                            val info = param.result as? PackageInfo
                            if (info != null) {
                                val originalCode = info.versionCode
                                val originalName = info.versionName

                                // Inject exact legit values into runtime memory registers
                                info.versionCode = LATEST_VERSION_CODE
                                info.versionName = LATEST_VERSION_NAME
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    info.setLongVersionCode(LATEST_VERSION_CODE_LONG)
                                }

                                param.result = info
                                log("$TAG Intercepted getPackageInfo! Code: $originalCode -> $LATEST_VERSION_CODE, Name: $originalName -> $LATEST_VERSION_NAME")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("$TAG Critical hook failure: ${t.message}")
        }
    }
}
