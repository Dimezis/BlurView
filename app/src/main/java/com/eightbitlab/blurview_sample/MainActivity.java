package com.eightbitlab.blurview_sample;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.DefaultBlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.viewPager) ViewPager viewPager;
    @BindView(R.id.tabLayout) TabLayout tabLayout;
    @BindView(R.id.blurView) BlurView blurView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupBlurView();
        setupViewPager();
    }

    private void setupViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupBlurView() {
        final int radius = 16;
        float scaleFactor = DefaultBlurController.DEFAULT_SCALE_FACTOR;

        final View decorView = getWindow().getDecorView();
        final View rootView = decorView.findViewById(android.R.id.content);
        final Drawable windowBackground = decorView.getBackground();

        final DefaultBlurController blurController = new DefaultBlurController(blurView, rootView, scaleFactor);
        blurController.setWindowBackground(windowBackground);
        blurController.setBlurAlgorithm(new RenderScriptBlur(this, true));
        blurController.setBlurRadius(radius);

        blurView.setBlurController(blurController);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (Page.values()[position]) {
                case FIRST:
                    return new ScrollFragment();
                case SECOND:
                    return new ListFragment();
                case THIRD:
                    return new ImageFragment();
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Page.values()[position].getTitle();
        }

        @Override
        public int getCount() {
            return Page.values().length;
        }
    }

    enum Page {
        FIRST("Tab1"),
        SECOND("Tab2"),
        THIRD("Tab3");

        private String title;

        Page(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }
}
