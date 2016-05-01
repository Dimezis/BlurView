package com.eightbitlab.blurview_sample;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.DefaultBlurController;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView viewToBlur = (TextView) findViewById(R.id.textView);
        final BlurView blurView = (BlurView) findViewById(R.id.blurView);

        setupBlurView(blurView);
        fillWithText(viewToBlur);
    }

    private void setupBlurView(BlurView blurView) {
        final View decorView = getWindow().getDecorView();
        final View rootView = decorView.findViewById(android.R.id.content);
        final Drawable windowBackground = decorView.getBackground();

        float scaleFactor = DefaultBlurController.DEFAULT_SCALE_FACTOR;
        DefaultBlurController blurController = new DefaultBlurController(blurView, rootView, scaleFactor);
        blurController.setWindowBackground(windowBackground);

        blurView.setBlurController(blurController);
    }

    private void fillWithText(TextView viewToBlur) {
        for (int i = 0; i < 100; i++) {
            viewToBlur.append("Blur Me! ");
        }
    }
}
