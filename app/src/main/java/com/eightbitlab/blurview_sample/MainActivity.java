package com.eightbitlab.blurview_sample;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderEffectPrecision;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private BlurView bottomBlurView;
    private BlurView topBlurView;
    private SeekBar radiusSeekBar;
    private ViewGroup root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setupBlurView();
        setupViewPager();
    }

    private void initView() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        bottomBlurView = findViewById(R.id.bottomBlurView);
        topBlurView = findViewById(R.id.topBlurView);
        radiusSeekBar = findViewById(R.id.radiusSeekBar);
        root = findViewById(R.id.root);
    }

    private void setupViewPager() {
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupBlurView() {
        final float radius = 25f;
        final float minBlurRadius = 10f;
        final float step = 4f;

        //set background, if your root layout doesn't have one
        final Drawable windowBackground = getWindow().getDecorView().getBackground();

        BlurAlgorithm algorithm;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            algorithm = new RenderEffectBlur(topBlurView, RenderEffectPrecision.EXACT);
        } else {
            algorithm = new RenderScriptBlur(this);
        }
        topBlurView.setupWith(root)
                .setFrameClearDrawable(windowBackground)
                .setBlurAlgorithm(algorithm)
                .setBlurRadius(radius);

        bottomBlurView.setupWith(root)
                .setFrameClearDrawable(windowBackground)
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(radius);

        int initialProgress = (int) (radius * step);
        radiusSeekBar.setProgress(initialProgress);

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBarListenerAdapter() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float blurRadius = progress / step;
                blurRadius = Math.max(blurRadius, minBlurRadius);
                topBlurView.setBlurRadius(blurRadius);
                bottomBlurView.setBlurRadius(blurRadius);
            }
        });
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
