package com.example.test.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.example.test.ui.fragment.HomeFragment

class NativeFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var homeFragment1: HomeFragment? = null


    fun setHomeFragment(homeFragment: HomeFragment) {
        this.homeFragment1 = homeFragment
    }

    //外部拦截法，当展示guideView时，不可以点击
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (homeFragment1?.isShowGuideDialog == true) true
        else super.onInterceptTouchEvent(ev)
    }
}