package com.shohan.cleanspace.shizuku

/**
 * This class runs INSIDE a privileged process started by Shizuku, with
 * shell-level (ADB) permission — not inside our normal app process.
 * It must have a public no-argument constructor. Do not reference any
 * Android Context / Application classes here.
 */
class CacheServiceImpl : ICacheService.Stub() {

    override fun runCommand(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
