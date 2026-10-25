package eightbitlab.com.blurview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Minimal host Activity for the resize-during-draw instrumented test.
 * The view hierarchy is built by the test itself so it can install a custom BlurTarget.
 */
public class ResizeDuringDrawTestActivity extends Activity {

    FrameLayout contentRoot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentRoot = new FrameLayout(this);
        setContentView(contentRoot);
    }
}
