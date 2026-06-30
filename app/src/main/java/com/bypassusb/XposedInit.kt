package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Deploying RIB-level interceptors...")
        val loader = lpparam.classLoader

        // =================================================================
        // 1. DATA MONITOR: Passive logging only (NO MUTATION = NO CRASH)
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.model.core.generated.rtapi.services.marketplacedriver.DriverCheckIssueData",
                loader,
                "type",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        if (result != null) {
                            XposedBridge.log("BypassUSB: 📊 Status evaluation detected: ${result.toString()}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            // Log quietly if missing
        }

        // =================================================================
        // 2. INTERACTOR ENGINE: Auto-Resolve Blocker Instances Instantly
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.ubercab.carbon.core.online_blockers.b",
                loader,
                "xl",      // Interactor didBecomeActive lifecycle stage
                "lf3.p",   // Target scope/worker parameter type
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("BypassUSB: 🚨 Blocker intercepted! Simulating manual resolution sequence...")
                        
                        try {
                            // Invoke the built-in self-resolution pipeline
                            XposedHelpers.callMethod(param.thisObject, "Pf")
                            XposedBridge.log("BypassUSB: ✨ Success! Called Pf() to clear blocker layout context.")
                        } catch (e: Throwable) {
                            XposedBridge.log("BypassUSB: ⚠️ Pf() invocation failed, trying fallback path (Pi)...")
                            try {
                                XposedHelpers.callMethod(param.thisObject, "Pi")
                                XposedBridge.log("BypassUSB: ✨ Success! Fallback layout clear executed.")
                            } catch (e2: Throwable) {
                                XposedBridge.log("BypassUSB: ❌ All programmatic bypass options exhausted: ${e2.message}")
                            }
                        }
                    }
                }
            )
            XposedBridge.log("BypassUSB: ✅ Interactor engine bypass hook bound successfully.")
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ❌ Failed to bind Interactor engine hook: ${t.message}")
        }
    }
}
