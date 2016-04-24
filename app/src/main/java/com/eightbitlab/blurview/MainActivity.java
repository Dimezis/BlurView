package com.eightbitlab.blurview;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

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
        final Drawable windowBackgroundDrawable = decorView.getBackground();
        blurView.setRootView(rootView);
        blurView.setWindowBackground(windowBackgroundDrawable);
    }

    private void fillWithText(TextView viewToBlur) {
        for (int i = 0; i < 300; i++) {
            viewToBlur.append("Blur Me! ");
        }
    }
}
