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

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Initializing layout suppression filters...")
        val loader = lpparam.classLoader

        val blockerViews = listOf(
            "com.ubercab.force_app_upgrade.ForceAppUpgradeView",
            "com.ubercab.driver_scheduler.online_offline.SchedulerOnlineBlockerView",
            "com.ubercab.carbon.core.online_blockers.OnlineBlockersView"
        )

        // =================================================================
        // 1. UI LAYER: Intercept and neutralize layout allocations
        // =================================================================
        for (viewClass in blockerViews) {
            // Hook Constructor 1: Programmatic instantiation (new View(context))
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
                XposedBridge.log("BypassUSB: ✅ Programmatic constructor hook set for $viewClass")
            } catch (t: Throwable) {
                // Class or constructor configuration variation
            }

            // Hook Constructor 2: Layout XML inflation instantiation
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
                XposedBridge.log("BypassUSB: ✅ XML inflation constructor hook set for $viewClass")
            } catch (t: Throwable) {
                // Class or constructor configuration variation
            }

            // Hook setVisibility to block post-creation display commands
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
                                param.args[0] = View.GONE // Force visibility state to GONE
                            }
                        }
                    }
                )
            } catch (t: Throwable) {
                // Fallback layer configuration hook variation
            }
        }
    }

    /**
     * Disables visibility state and interaction parameters for target views
     */
    private fun suppressView(view: View, className: String) {
        XposedBridge.log("BypassUSB: 💥 Suppression filter triggered for layout instance: $className")
        
        view.visibility = View.GONE
        view.isClickable = false
        view.isFocusable = false

        view.post {
            try {
                val parent = view.parent as? ViewGroup
                parent?.removeView(view)
                XposedBridge.log("BypassUSB: 🗑️ Removed layout block context from window tree hierarchy")
            } catch (e: Throwable) {
                // Container not populated or detached
            }
        }
    }
}
