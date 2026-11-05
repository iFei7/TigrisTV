package com.leslie.tvhome

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreensaverScope.register(this)
        I18n.initialize(this)
        // A home screen is a direct-boot component, so this process comes up at
        // LOCKED_BOOT_COMPLETED, before credential-encrypted storage is readable. Reading a
        // preference here threw IllegalStateException and took the whole process down (twice per
        // boot: once for the accessibility service, once for the boot receiver). Everything below
        // needs that storage, so it waits for the unlock pass — see HomeActivity.showLockedCover.
        if (Guard.isUserUnlocked(this)) onUserUnlocked()
    }

    /**
     * Work that needs credential-encrypted storage.
     *
     * Runs from [onCreate] on a normal start, and again from the home screen the moment the user
     * unlocks at boot. Safe to call more than once: the launch check is time-stamped.
     */
    fun onUserUnlocked() {
        Updates.schedule(this)
        val sp = Updates.prefs(this)
        if (System.currentTimeMillis() - sp.getLong("lastLaunchCheck", 0) > 6 * 60 * 60 * 1000L) {
            sp.edit().putLong("lastLaunchCheck", System.currentTimeMillis()).apply()
            Updates.executor.execute { Updates.check(this) }
        }
    }
}
