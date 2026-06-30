package com.bypassusb

import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.util.AttributeSet
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        XposedBridge.log("BypassUSB: 🎯 Target matched. Destroying core Blocker UI layers...")
        val loader = lpparam.classLoader

        // Added the core BlockerView class you uncovered to our target list
        val blockerViews = listOf(
            "com.uber.blockers.core.rib.BlockersView",
            "com.ubercab.force_app_upgrade.ForceAppUpgradeView",
            "com.ubercab.driver_scheduler.online_offline.SchedulerOnlineBlockerView",
            "com.ubercab.carbon.core.online_blockers.OnlineBlockersView"
        )

        for (viewClass in blockerViews) {
            // Hook programmatic view instantiation
            try {
                XposedHelpers.findAndHookConstructor(
                    viewClass,
                    loader,
                    Context::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val view = param.thisObject as View
                            suppressView(view, viewClass)
                        }
                    }
                )
                XposedBridge.log("BypassUSB: ✅ Programmatic constructor hook armed: $viewClass")
            } catch (t: Throwable) {
                // Class or constructor variant mismatch
            }

            // Hook XML layout inflation instantiation
            try {
                XposedHelpers.findAndHookConstructor(
                    viewClass,
                    loader,
                    Context::class.java,
                    AttributeSet::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val view = param.thisObject as View
                            suppressView(view, viewClass)
                        }
                    }
                )
                XposedBridge.log("BypassUSB: ✅ XML inflation constructor hook armed: $viewClass")
            } catch (t: Throwable) {
                // Class or constructor variant mismatch
            }

            // Lock the visibility state down to ensure it stays hidden
            try {
                XposedHelpers.findAndHookMethod(
                    viewClass,
                    loader,
                    "setVisibility",
                    Int::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val visibility = param.args[0] as Int
                            if (visibility == View.VISIBLE) {
                                param.args[0] = View.GONE
                            }
                        }
                    }
                )
            } catch (t: Throwable) {
                // Class method variant mismatch
            }
        }
    }

    private fun suppressView(view: View, className: String) {
        XposedBridge.log("BypassUSB: 💥 Suppressing target blocker layout instance -> $className")
        
        view.visibility = View.GONE
        view.isClickable = false
        view.isFocusable = false

        // Rip the layout component out of the view rendering hierarchy tree
        view.post {
            try {
                val parent = view.parent as? ViewGroup
                parent?.removeView(view)
                XposedBridge.log("BypassUSB: 🗑️ Successfully detached $className from parent layout container.")
            } catch (e: Throwable) {
                // Parent container not active yet
            }
        }
    }
}
