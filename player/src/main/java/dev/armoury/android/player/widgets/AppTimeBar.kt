package dev.armoury.android.player.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.exoplayer2.ui.DefaultTimeBar

open class AppTimeBar : DefaultTimeBar {

    private var forceVisibility : Int? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr, attrs)

    override fun setVisibility(visibility: Int) {
        forceVisibility?.let {super.setVisibility(it)}
            ?: super.setVisibility(visibility)
    }

    fun setForceVisibility(visibility: Int?) {
        this.forceVisibility = visibility
        visibility?.let {
            if (this.forceVisibility != View.VISIBLE) setVisibility(it)
        }
    }

}