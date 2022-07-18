package eightbitlab.com.blurview;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

//Used in edit mode and in case if no BlurController was set
class NoOpController implements BlurController {
    @Override
    public boolean draw(Canvas canvas) {
        return true;
    }

    @Override
    public void updateBlurViewSize() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        return this;
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable Drawable windowBackground) {
        return this;
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        return this;
    }

    @Override
    public BlurViewFacade setBlurAutoUpdate(boolean enabled) {
        return this;
    }
}
