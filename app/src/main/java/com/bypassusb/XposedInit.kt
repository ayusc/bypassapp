package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge.log

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        try {
            // Hook: com.ubercab.force_app_upgrade.c.b(Ljava/lang/Object;)Z
            findAndHookMethod(
                "com.ubercab.force_app_upgrade.c",
                lpparam.classLoader,
                "b",
                java.lang.Object::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false  // Bypass update required screen
                    }
                }
            )

            // Log success — visible in LSPosed > Modules > Logs
            log("BypassUSB: ✅ Uber force upgrade check bypassed")

        } catch (t: Throwable) {
            log("BypassUSB: ❌ Hook failed: ${t.message}")
            log(t)
        }
    }
}
