package com.example.test.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.test.R


class SettingItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    RelativeLayout(context, attrs, defStyleAttr) {

    var itemIcon:AppCompatImageView
    var itemTitle:AppCompatTextView

    init {
        LayoutInflater.from(context).inflate(R.layout.setting_item_layout,this)
        itemIcon = findViewById(R.id.setting_item_icon)
        itemTitle = findViewById(R.id.setting_item_title)
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SettingItemView,
            defStyleAttr,
            0
        )
        itemIcon.setImageDrawable(ContextCompat.getDrawable(context,typedArray.getResourceId(R.styleable.SettingItemView_icon,R.mipmap.ic_launcher)))
        itemTitle.text = typedArray.getString(R.styleable.SettingItemView_title)
        typedArray.recycle()
    }

}
