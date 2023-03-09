package com.example.test.ui.widget.guideview;

import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.test.R;

public class SimpleComponent implements Component {

    @Override public View getView(LayoutInflater inflater) {

        ConstraintLayout ll = (ConstraintLayout) inflater.inflate(R.layout.fragment_home, null);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            }
        });
        return ll;
    }

    @Override public int getAnchor() {
        return Component.ANCHOR_BOTTOM;
    }

    @Override public int getFitPosition() {
        return Component.FIT_END;
    }

    @Override public int getXOffset() {
        return 0;
    }

    @Override public int getYOffset() {
        return 10;
    }
}

