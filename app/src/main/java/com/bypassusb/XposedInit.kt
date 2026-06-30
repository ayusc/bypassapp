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

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Initializing hook registry...")
        val loader = lpparam.classLoader

        // The 3 layout views Uber uses to build update/online blocker walls
        val blockerViews = listOf(
            "com.ubercab.force_app_upgrade.ForceAppUpgradeView",
            "com.ubercab.driver_scheduler.online_offline.SchedulerOnlineBlockerView",
            "com.ubercab.carbon.core.online_blockers.OnlineBlockersView"
        )

        // =================================================================
        // STRATEGY 1: UI Constructor Nuke
        // =================================================================
        for (viewClass in blockerViews) {
            try {
                // Hook the standard Android View constructor used by XML layouts
                XposedHelpers.findAndHookConstructor(
                    viewClass,
                    loader,
                    Context::class.java,
                    AttributeSet::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val view = param.thisObject as View
                            XposedBridge.log("BypassUSB: 💥 CRITICAL hit! Blocker view created: $viewClass")

                            // Force the view to disappear completely and drop inputs
                            view.visibility = View.GONE
                            view.isClickable = false
                            view.isFocusable = false

                            // Post-routine: Safely rip the view out of its parent layout
                            view.post {
                                try {
                                    val parent = view.parent as? ViewGroup
                                    parent?.removeView(view)
                                    XposedBridge.log("BypassUSB: 🗑️ Successfully deleted $viewClass from window hierarchy")
                                } catch (e: Throwable) {
                                    // Parent layout not ready yet, skip quietly
                                }
                            }
                        }
                    }
                )
                XposedBridge.log("BypassUSB: ✅ UI Hook successfully registered for: $viewClass")
            } catch (t: Throwable) {
                XposedBridge.log("BypassUSB: ⚠️ UI Hook registration failed for $viewClass: ${t.message}")
            }
        }

        // =================================================================
        // STRATEGY 2: Verbose Data-Layer Tracker
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.model.core.generated.rtapi.services.marketplacedriver.DriverCheckIssueData",
                loader,
                "type",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        XposedBridge.log("BypassUSB: 📊 DriverCheckIssueData.type() evaluated to: $result")
                        
                        if (result != null && result.toString() == "FORCE_UPGRADE") {
                            param.result = null
                            XposedBridge.log("BypassUSB: 🛡️ Overrode Data-Layer status to NULL")
                        }
                    }
                }
            )
            XposedBridge.log("BypassUSB: ✅ Data-layer Tracker successfully registered")
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Data-layer Tracker registration failed: ${t.message}")
        }
    }
}
