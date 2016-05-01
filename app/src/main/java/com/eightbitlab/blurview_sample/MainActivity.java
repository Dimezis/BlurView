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
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.DefaultBlurController;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBlurView();
        setupViewPager();
    }

    private void setupViewPager() {
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupBlurView() {
        final BlurView blurView = (BlurView) findViewById(R.id.blurView);

        final View decorView = getWindow().getDecorView();
        final View rootView = decorView.findViewById(android.R.id.content);
        final Drawable windowBackground = decorView.getBackground();

        float scaleFactor = DefaultBlurController.DEFAULT_SCALE_FACTOR;
        DefaultBlurController blurController = new DefaultBlurController(blurView, rootView, scaleFactor);
        blurController.setWindowBackground(windowBackground);

        blurView.setBlurController(blurController);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new ListFragment();
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
