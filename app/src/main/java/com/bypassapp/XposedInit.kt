package com.bypassapp

import android.content.pm.PackageInfo
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.ArrayList

class XposedInit : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PACKAGE = "com.lyft.android.driver"
        private const val TAG = "[lyft]"
        
        // Legit values from your latest configuration
        private const val LATEST_VERSION_CODE = 1782286115
        private const val LATEST_VERSION_CODE_LONG = 1782286115L
        private const val LATEST_VERSION_NAME = "2026.24.3.1782286115"
    }

    private var checklistHooksApplied = false
    private var onboardingHooksApplied = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return
        if (lpparam.processName != TARGET_PACKAGE) return

        log("$TAG Target application process loaded: ${lpparam.packageName}")

        // 1. Version Spoofing Hook (Confirmed working)
        hookPackageInfo(lpparam)

        // 2. Immediate baseline execution check
        tryArmHooks(lpparam.classLoader)

        // 3. Activity Lifecycle Monitor (Catches class loaders when onboarding screens initialize)
        hookInstrumentationLifecycle(lpparam)

        // 4. Double-Overload ClassLoader Interception
        hookClassLoader(lpparam)
    }

    private fun hookPackageInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val requestedPackage = param.args[0] as? String

                        if (requestedPackage != null && requestedPackage == lpparam.packageName) {
                            val info = param.result as? PackageInfo
                            if (info != null) {
                                info.versionCode = LATEST_VERSION_CODE
                                info.versionName = LATEST_VERSION_NAME
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    info.setLongVersionCode(LATEST_VERSION_CODE_LONG)
                                }

                                param.result = info
                                log("$TAG Intercepted getPackageInfo Version Updated.")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("$TAG PackageInfo hook failure: ${t.message}")
        }
    }

    private fun hookInstrumentationLifecycle(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Intercepting layout inflation initialization routines across any dynamic layout context
            XposedHelpers.findAndHookMethod(
                "android.app.Instrumentation",
                lpparam.classLoader,
                "callActivityOnCreate",
                "android.app.Activity",
                "android.os.Bundle",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.args[0] as? android.app.Activity
                        if (activity != null) {
                            // Run deep validation using the active Activity Context ClassLoader
                            tryArmHooks(activity.classLoader)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("$TAG Instrumentation lifecycle tracking hook failed: ${t.message}")
        }
    }

    private fun hookClassLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val classLoaderClass = XposedHelpers.findClass("java.lang.ClassLoader", lpparam.classLoader)

            // Overload 1: loadClass(String)
            XposedHelpers.findAndHookMethod(
                classLoaderClass,
                "loadClass",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activeLoader = param.thisObject as? ClassLoader ?: return
                        tryArmHooks(activeLoader)
                    }
                }
            )

            // Overload 2: loadClass(String, boolean) -> Frequently used internally by custom Dex class loaders
            XposedHelpers.findAndHookMethod(
                classLoaderClass,
                "loadClass",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activeLoader = param.thisObject as? ClassLoader ?: return
                        tryArmHooks(activeLoader)
                    }
                }
            )
        } catch (t: Throwable) {
            log("$TAG ClassLoader listener hook failure: ${t.message}")
        }
    }

    private fun tryArmHooks(classLoader: ClassLoader) {
        // Safe check for Checklist UI components
        if (!checklistHooksApplied) {
            val classI = XposedHelpers.findClassIfExists("bm2.i", classLoader)
            val classH = XposedHelpers.findClassIfExists("bm2.h", classLoader)
            if (classI != null || classH != null) {
                applyChecklistHooks(classLoader)
            }
        }

        // Safe check for Driver Onboarding modules
        if (!onboardingHooksApplied) {
            val onboardingTarget = XposedHelpers.findClassIfExists("com.lyft.android.driver.onboarding.checklist.screens.b\$k", classLoader)
            if (onboardingTarget != null) {
                applyOnboardingHooks(classLoader)
            }
        }
    }

    private fun applyChecklistHooks(classLoader: ClassLoader) {
        try {
            checklistHooksApplied = true
            log("$TAG Deploying checklist filtration hooks...")

            XposedHelpers.findAndHookMethod(
                "bm2.i",
                classLoader,
                "a",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalList = param.result as? List<*> ?: return
                        param.result = filterListItems(originalList)
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "bm2.h",
                classLoader,
                "b",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalList = param.result as? List<*> ?: return
                        param.result = filterListItems(originalList)
                    }
                }
            )
        } catch (t: Throwable) {
            checklistHooksApplied = false
            log("$TAG Failed to apply checklist hooks: ${t.message}")
        }
    }

    private fun filterListItems(originalList: List<*>): ArrayList<Any> {
        val cleanList = ArrayList<Any>()
        for (item in originalList) {
            if (item != null) {
                val text = item.toString().lowercase()
                if (text.contains("confirm")) {
                    continue
                }
                cleanList.add(item)
            }
        }
        return cleanList
    }

    private fun applyOnboardingHooks(classLoader: ClassLoader) {
        try {
            onboardingHooksApplied = true
            log("$TAG Deploying onboarding screen manipulation hooks...")

            // 1. Empty implementation bypass for SectionContainer.e
            try {
                val containerClass = XposedHelpers.findClass(
                    "com.lyft.android.driver.onboarding.checklist.plugins.checklist.section.container.a",
                    classLoader
                )
                for (method in containerClass.declaredMethods) {
                    if (method.name == "e" && method.parameterTypes.size == 3) {
                        XposedHelpers.findAndHookMethod(
                            containerClass,
                            "e",
                            method.parameterTypes[0],
                            method.parameterTypes[1],
                            method.parameterTypes[2],
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.result = null
                                }
                            }
                        )
                        break
                    }
                }
            } catch (e: Throwable) { log("$TAG Container method hook failed: ${e.message}") }

            // 2. Force true on DriverInfoImpl (ig0.f) state flags
            val stateMethods = listOf("a", "d", "l")
            for (methodName in stateMethods) {
                try {
                    XposedHelpers.findAndHookMethod(
                        "ig0.f",
                        classLoader,
                        methodName,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = true
                            }
                        }
                    )
                } catch (e: Throwable) { /* Ignore non-existent methods */ }
            }

            // 3. Custom Click Event Generator on Button Handler
            XposedHelpers.findAndHookMethod(
                "com.lyft.android.driver.onboarding.checklist.screens.b\$k",
                classLoader,
                "onClick",
                View::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val handlerInstance = param.thisObject

                        val screenBInstance = XposedHelpers.getObjectField(handlerInstance, "b")

                        val subflowActionClass = XposedHelpers.findClass("bm2.n", classLoader)
                        val fakeAction = XposedHelpers.newInstance(subflowActionClass, "lyft://home", "completed", false)

                        val responseFClass = XposedHelpers.findClass("bm2.c\$f", classLoader)
                        val fakeResponse = XposedHelpers.newInstance(responseFClass, fakeAction)

                        XposedHelpers.callMethod(screenBInstance, "J", fakeResponse, false)

                        param.result = null
                    }
                }
            )

        } catch (t: Throwable) {
            onboardingHooksApplied = false
            log("$TAG Onboarding flows hook injection failed: ${t.message}")
        }
    }
}
