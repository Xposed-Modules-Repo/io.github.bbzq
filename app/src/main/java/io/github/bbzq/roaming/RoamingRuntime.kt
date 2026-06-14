package io.github.bbzq.roaming

import android.content.Context
import android.content.SharedPreferences
import io.github.bbzq.ModuleSettingsBridge
import io.github.bbzq.roaming.hook.FreeCopyHook
import io.github.bbzq.roaming.hook.RewardAdHook
import io.github.bbzq.roaming.hook.SettingHook
import io.github.bbzq.roaming.hook.ShareHook
import io.github.bbzq.roaming.hook.SplashAdHook
import io.github.bbzq.roaming.hook.StoryPlayerAdHook
import io.github.libxposed.api.XposedInterface

object RoamingRuntime {
    fun start(
        xposed: XposedInterface,
        packageName: String,
        processName: String,
        application: Context,
        classLoader: ClassLoader,
        log: (String, Throwable?) -> Unit,
    ) {
        val env = RoamingEnv(
            xposed = xposed,
            packageName = packageName,
            processName = processName,
            hostContext = application.applicationContext ?: application,
            classLoader = classLoader,
            logger = log,
        )

        env.log("BBZQ runtime starting for $packageName/$processName")

        val hooks = when {
            processName.endsWith(":web") -> listOf(
                ::ShareHook,
                ::RewardAdHook,
            )

            processName.endsWith(":download") -> emptyList()

            else -> listOf(
                ::SettingHook,
                ::SplashAdHook,
                ::ShareHook,
                ::FreeCopyHook,
                ::StoryPlayerAdHook,
                ::RewardAdHook,
            )
        }

        hooks.forEach { factory ->
            val hook = factory(env)
            runCatching { hook.startHook() }
                .onFailure { env.log("Hook failed: ${hook.javaClass.simpleName}", it) }
        }

        env.log("BBZQ runtime installed ${hooks.size} hook(s)")
    }
}

class RoamingEnv(
    val xposed: XposedInterface,
    val packageName: String,
    val processName: String,
    val hostContext: Context,
    val classLoader: ClassLoader,
    private val logger: (String, Throwable?) -> Unit,
) {
    val prefs: SharedPreferences
        get() = ModuleSettingsBridge.instance

    fun log(message: String, throwable: Throwable? = null) {
        logger(message, throwable)
    }

    companion object
}

abstract class BaseRoamingHook(
    protected val env: RoamingEnv,
) {
    protected val xposed: XposedInterface
        get() = env.xposed

    protected val classLoader: ClassLoader
        get() = env.classLoader

    protected val prefs: SharedPreferences
        get() = env.prefs

    protected fun log(message: String, throwable: Throwable? = null) {
        env.log(message, throwable)
    }

    abstract fun startHook()
}
