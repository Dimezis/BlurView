package com.eightbitlab.blurview_sample;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import eightbitlab.com.blurview.gl.GLBlurView;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private GLBlurView blurView;
    private ViewGroup root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setupBlurView();
        setupViewPager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        blurView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        blurView.onStop();
    }

    private void initView() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        blurView = findViewById(R.id.blurView);
        root = findViewById(R.id.root);
    }

    private void setupViewPager() {
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupBlurView() {
        //set background, if your root layout doesn't have one
        final Drawable windowBackground = getWindow().getDecorView().getBackground();

        blurView.setRootView(root);
        blurView.setWindowBackground(windowBackground);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {

        ViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return Page.values()[position].getFragment();
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
        FIRST("ScrollView") {
            @Override
            Fragment getFragment() {
                return new ScrollFragment();
            }
        },
        SECOND("RecyclerView") {
            @Override
            Fragment getFragment() {
                return new ListFragment();
            }
        },
        THIRD("Static") {
            @Override
            Fragment getFragment() {
                return new ImageFragment();
            }
        };

        private String title;

        Page(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }

        abstract Fragment getFragment();
    }
}
