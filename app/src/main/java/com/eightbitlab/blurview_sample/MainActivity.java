package com.eightbitlab.blurview_sample;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private BlurTarget target;
    private TabLayout tabLayout;
    private BlurView bottomBlurView;
    private BlurView topBlurView;
    private SeekBar radiusSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setupBlurView();
        setupViewPager();
        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(bottomBlurView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomBlurView.setPadding(0, 0, 0, insets.bottom);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topBlurView.getLayoutParams();
            params.topMargin = (int) (insets.top * 1.5);
            topBlurView.setLayoutParams(params);

            return windowInsets;
        });
    }

    private void initView() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        bottomBlurView = findViewById(R.id.bottomBlurView);
        topBlurView = findViewById(R.id.topBlurView);
        target = findViewById(R.id.target);
        // Rounded corners + casting elevation shadow with transparent background
        topBlurView.setClipToOutline(true);
        topBlurView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                topBlurView.getBackground().getOutline(outline);
                outline.setAlpha(1f);
            }
        });
        radiusSeekBar = findViewById(R.id.radiusSeekBar);
    }

    private void setupViewPager() {
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupBlurView() {
        final float radius = 25f;
        final float minBlurRadius = 4f;
        final float step = 4f;

        //set background, if your root layout doesn't have one
        final Drawable windowBackground = getWindow().getDecorView().getBackground();
        topBlurView.setupWith(target)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);

        bottomBlurView.setupWith(target)
                .setFrameClearDrawable(windowBackground)
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
