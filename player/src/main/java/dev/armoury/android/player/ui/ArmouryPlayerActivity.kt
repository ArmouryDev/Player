package dev.armoury.android.player.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.databinding.ViewDataBinding
import dev.armoury.android.data.ArmouryUiAction
import dev.armoury.android.ui.ArmouryActivity
import dev.armoury.android.utils.isPortrait
import dev.armoury.android.viewmodel.ArmouryViewModel

/**
 * For now we are considering that applications are going to be implemented
 * in the Portrait-Mode only. TODO Later we should make this class more general
 */
abstract class ArmouryPlayerActivity<UA: ArmouryUiAction, T : ViewDataBinding, V : ArmouryViewModel<UA>> :
    ArmouryActivity<UA, T, V>() {

    protected abstract fun getToolbar(): Toolbar?

    //  TODO Should be removed later
    private var systemVisibility: Int = -1

    override fun doOtherTasks() {
        systemVisibility = window.decorView.systemUiVisibility
    }

    /**
     * If the user is pressed the back-button and the application is in landscape mode,
     * we should make the screen portrait. Otherwise, we should let the OS decides what
     * to do
     */
    override fun onBackPressed() {
        if (isPortrait()) super.onBackPressed()
        else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (!hasToggleFullScreenButton()) super.onBackPressed()
        }
    }

    /**
     * If the current activity become focused again, and the application is in the
     * landscape mode, we should hide the navigation bar.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isPortrait()) {
            toggleNavigationBarVisibility(show = false)
        }
    }

    /**
     * If the application become portrait, we should show the toolbar and
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isPortrait()) {
            toggleNavigationBarVisibility()
            getToolbar()?.visibility = View.VISIBLE
        } else {
            toggleNavigationBarVisibility(show = false)
            getToolbar()?.visibility = View.GONE
        }
    }

    protected open fun hasToggleFullScreenButton() : Boolean = true

    private fun toggleNavigationBarVisibility(show: Boolean = true) {
        if (show) {
            window.decorView.systemUiVisibility = systemVisibility
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
        //  TODO Not working properly
        //  TODO Should be checked later
//        WindowCompat.setDecorFitsSystemWindows(window, show)
    }

}