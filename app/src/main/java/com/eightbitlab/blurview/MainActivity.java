package com.eightbitlab.blurview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView viewToBlur = (TextView) findViewById(R.id.textView);
        final BlurView blurView = (BlurView) findViewById(R.id.blurView);

        fillWithText(viewToBlur);
    }

    private void fillWithText(TextView viewToBlur) {
        for (int i = 0; i < 300; i++) {
            viewToBlur.append("Blur Me! ");
        }
    }
}
