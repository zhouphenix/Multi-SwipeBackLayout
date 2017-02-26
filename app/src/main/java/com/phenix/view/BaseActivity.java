package com.phenix.view;

import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import lib.phenix.com.views.SwipeBackLayout;

/**
 * @author zhouphenix on 2017-2-25.
 */

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View contentView = getLayoutInflater().inflate(layoutResID, null, false);
        this.setContentView(contentView);
    }


    @Override
    public void setContentView(View view) {
        SwipeBackLayout swipeBackLayout = new SwipeBackLayout(this, view, SwipeBackLayout.UP | SwipeBackLayout.LEFT | SwipeBackLayout.RIGHT | SwipeBackLayout.DOWN);
        swipeBackLayout.setOnSwipeBackListener(new SwipeBackLayout.OnSwipeBackListener() {
            @Override
            public boolean onIntercept(@SwipeBackLayout.DragDirection int direction, float x, float y) {
                return onSwipeBackPre(direction, x, y);
            }

            @Override
            public void onViewPositionChanged(float fraction) {
            }

            @Override
            public void onAnimationEnd() {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            }
        });
        super.setContentView(swipeBackLayout);
    }

    public boolean onSwipeBackPre(@SwipeBackLayout.DragDirection int direction, float x, float y){
        return false;
    }
}
