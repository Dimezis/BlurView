package com.eightbitlab.blurview;

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

//        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        View rootView = getWindow().getDecorView().getRootView();

        blurView.setRootView(rootView);
        final View scrollView = findViewById(R.id.scrollView);

        blurView.setDependencyView(scrollView);

        fillWithText(viewToBlur);
    }

    private void fillWithText(TextView viewToBlur) {
        for (int i = 0; i < 300; i++) {
            viewToBlur.append("Blur Me! ");
        }
    }
}
