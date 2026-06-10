package io.github.bzzq.hooks

import io.github.bzzq.ModuleSettings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class BlockLiveReservationHook(
    override val targetPackageName: String,
) : AppHook {
    override fun install(xposed: XposedInterface, packageReady: PackageReadyParam, log: (String, Throwable?) -> Unit) {
        val classLoader = packageReady.getClassLoader()
        val prefs = xposed.getRemotePreferences(ModuleSettings.PREFS_NAME)

        // Hook com.bapis.bilibili.app.view.v1.ViewReply$Builder.build() to clear live reservation info
        // This follows the logic of calling clearLiveOrderInfo() on the result/builder.
        runCatching {
            val builderClass = Class.forName("com.bapis.bilibili.app.view.v1.ViewReply\$Builder", false, classLoader)
            val buildMethod = builderClass.getDeclaredMethod("build")
            xposed.hook(buildMethod).intercept { chain ->
                val builder = chain.thisObject
                if (builder != null && ModuleSettings.isBlockLiveReservationEnabled(prefs)) {
                    runCatching {
                        val clearMethod = builder.javaClass.getDeclaredMethod("clearLiveOrderInfo")
                        clearMethod.invoke(builder)
                    }
                }
                chain.proceed()
            }
            log("Installed ViewReply\$Builder.build() hook for block live reservation", null)
        }.onFailure {
            // Silently fail if class or method is not found
        }
        
        // Also hook the message class directly for redundancy or if the app uses the message directly
        runCatching {
            val viewReplyClass = Class.forName("com.bapis.bilibili.app.view.v1.ViewReply", false, classLoader)
            
            val hasLiveOrderInfo = viewReplyClass.getDeclaredMethod("hasLiveOrderInfo")
            xposed.hook(hasLiveOrderInfo).intercept { chain ->
                if (ModuleSettings.isBlockLiveReservationEnabled(prefs)) {
                    false
                } else {
                    chain.proceed()
                }
            }
            log("Installed ViewReply.hasLiveOrderInfo() hook for block live reservation", null)
        }.onFailure {
            // Silently fail
        }
    }
}
