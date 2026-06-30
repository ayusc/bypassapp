package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Initializing navigation stack filters...")
        val loader = lpparam.classLoader

        // =================================================================
        // 1. DATA LAYER: Keep stream desensitization active
        // =================================================================
        try {
            val optionalClass = XposedHelpers.findClass("com.google.common.base.Optional", loader)
            val absentOptional = XposedHelpers.callStaticMethod(optionalClass, "absent")
            val dataAccessors = listOf("a", "b", "c", "d", "e", "f")

            for (methodName in dataAccessors) {
                XposedHelpers.findAndHookMethod(
                    "wk5.f",
                    loader,
                    methodName,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = absentOptional
                        }
                    }
                )
            }
            XposedBridge.log("BypassUSB: 🛡️ Stream desensitization hooks active on wk5.f")
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ❌ Data layer filter setup failed: ${t.message}")
        }

        // =================================================================
        // 2. NAVIGATION LAYER: Force pop the empty layout layer off the stack
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.blockers.core.rib.b",
                loader,
                "Il",
                "com.uber.blockers.core.rib.b",
                "wk5.f",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val interactor = param.args[0]
                        
                        try {
                            // Extract the router instance from the interactor using Cl()
                            val router = XposedHelpers.callMethod(interactor, "Cl")
                            if (router != null) {
                                val routerClass = XposedHelpers.findClass("com.uber.blockers.core.rib.BlockersRouter", loader)
                                
                                // Invoke the static popper method A0(BlockersRouter)
                                XposedHelpers.callStaticMethod(routerClass, "A0", router)
                                XposedBridge.log("BypassUSB: ✨ Success! Popped ghost Blocker screen layer off the navigation stack.")
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("BypassUSB: ⚠️ Screen stack ejection cycle bypassed: ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("BypassUSB: ✅ Navigation stack popper successfully armed")
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ❌ Navigation stack popper setup failed: ${t.message}")
        }
    }
}
