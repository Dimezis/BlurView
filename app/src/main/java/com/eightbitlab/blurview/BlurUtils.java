package com.eightbitlab.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;

public final class BlurUtils {

    public static void blur(Bitmap background, View view, Context context) {
        final float radius = 20;

        Bitmap overlay = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(overlay);
        canvas.translate(-view.getLeft(), -view.getTop());
        canvas.drawBitmap(background, 0, 0, null);

        RenderScript rs = RenderScript.create(context);

        Allocation overlayAlloc = Allocation.createFromBitmap(rs, overlay);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());

        blur.setInput(overlayAlloc);
        blur.setRadius(radius);
        blur.forEach(overlayAlloc);

        overlayAlloc.copyTo(overlay);

        view.setBackground(new BitmapDrawable(context.getResources(), overlay));

        rs.destroy();
    }
}
