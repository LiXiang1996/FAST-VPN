package com.example.test.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.test.R


class TitleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    var leftImg: AppCompatImageView
    var rightImg: AppCompatImageView
    var centerTitle: AppCompatTextView
    var rightTitle: AppCompatTextView

    init {
        LayoutInflater.from(context).inflate(R.layout.title_view_layout, this)
        leftImg = findViewById(R.id.title_view_left_icon)
        centerTitle = findViewById(R.id.title_view_title)
        rightTitle = findViewById(R.id.title_view_right_text)
        rightImg = findViewById(R.id.title_view_right_icon)
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.TitleView, defStyleAttr, 0
        )
        leftImg.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                typedArray.getResourceId(R.styleable.TitleView_leftImg, R.mipmap.icon_back_arrow)
            )
        )
        rightImg.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                typedArray.getResourceId(R.styleable.TitleView_rightImg, R.mipmap.icon_back_arrow)
            )
        )
        centerTitle.text = typedArray.getString(R.styleable.TitleView_centerTitle)
        rightTitle.text = typedArray.getString(R.styleable.TitleView_rightText)

        typedArray.recycle()
    }


    fun setVisible(
        leftImgVisible: Boolean,
        centerTitleVisible: Boolean,
        rightImgVisible: Boolean,
        rightTitleVisible: Boolean
    ) {
        leftImg.isVisible = leftImgVisible
        centerTitle.isVisible = centerTitleVisible
        rightImg.isVisible = rightImgVisible
        rightTitle.isVisible = rightTitleVisible
    }

    fun setRightTextColor(resId: Int) {
        rightTitle.setTextColor(resId)
    }


}
