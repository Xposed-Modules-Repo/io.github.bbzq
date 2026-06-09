package io.github.bzzq

import android.util.Log
import io.github.bzzq.hooks.HookRegistry
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class BzzqModule : XposedModule() {
    private var isSupportedFramework = false

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        val frameworkName = getFrameworkName()
        val frameworkVersionCode = getFrameworkVersionCode()
        isSupportedFramework = isSupportedFramework(frameworkName, frameworkVersionCode)
        val status = if (isSupportedFramework) "enabled" else "disabled"
        log(
            Log.INFO,
            LOG_TAG,
            "Loaded in ${param.getProcessName()} on $frameworkName($frameworkVersionCode); bzzq is $status",
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!isSupportedFramework) return

        HookRegistry.handlePackageReady(this, param) { message, throwable ->
            if (throwable == null) {
                log(Log.INFO, LOG_TAG, message)
            } else {
                log(Log.WARN, LOG_TAG, message, throwable)
            }
        }
    }

    private fun isSupportedFramework(frameworkName: String, frameworkVersionCode: Long): Boolean {
        return frameworkName.equals(NPATCH_FRAMEWORK_NAME, ignoreCase = true) ||
            frameworkName.equals(VECTOR_FRAMEWORK_NAME, ignoreCase = true) ||
            (
                frameworkName.equals(LSPOSED_FRAMEWORK_NAME, ignoreCase = true) &&
                    frameworkVersionCode > MIN_LSPOSED_VERSION_CODE
                )
    }

    private companion object {
        private const val LOG_TAG = "bzzq"
        private const val NPATCH_FRAMEWORK_NAME = "NPatch"
        private const val VECTOR_FRAMEWORK_NAME = "Vector"
        private const val LSPOSED_FRAMEWORK_NAME = "LSPosed"
        private const val MIN_LSPOSED_VERSION_CODE = 7700L
    }
}
