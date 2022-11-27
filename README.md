[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://vshymanskyy.github.io/StandWithUkraine)

# BlurView

<a href="url"><img src="https://user-images.githubusercontent.com/1433500/174389657-f52837db-005b-4a68-b9c6-ce196fa03395.jpg" width="432" ></a>

Dynamic iOS-like blur for Android Views. Includes library and small example project.

BlurView can be used as a regular FrameLayout. It blurs its underlying content and draws it as a
background for its children. The children of the BlurView are not blurred. BlurView redraws its
blurred content when changes in view hierarchy are detected (draw() called). It honors its position
and size changes, including view animation and property animation.

## How to use
```XML
  <eightbitlab.com.blurview.BlurView
      android:id="@+id/blurView"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:blurOverlayColor="@color/colorOverlay">

       <!--Any child View here, TabLayout for example. This View will NOT be blurred -->

  </eightbitlab.com.blurview.BlurView>
```

```Java
    float radius = 20f;

    View decorView = getWindow().getDecorView();
    // ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
    ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
    
    // Optional:
    // Set drawable to draw in the beginning of each blurred frame.
    // Can be used in case your layout has a lot of transparent space and your content
    // gets a too low alpha value after blur is applied.
    Drawable windowBackground = decorView.getBackground();

    blurView.setupWith(rootView, new RenderScriptBlur(this)) // or RenderEffectBlur
           .setFrameClearDrawable(windowBackground) // Optional
           .setBlurRadius(radius)
```

Always try to choose the closest possible root layout to BlurView. This will greatly reduce the amount of work needed for creating View hierarchy snapshot.

## SurfaceView, TextureView, VideoView, MapFragment, GLSurfaceView, etc
BlurView currently doesn't support blurring of these targets, because they work only with hardware-accelerated Canvas, and BlurView relies on a software Canvas to make a snapshot of Views to blur.

## Gradle

Since JCenter is closing, please use https://jitpack.io/ and release tags as the source of stable
artifacts.
```Groovy
implementation 'com.github.Dimezis:BlurView:version-2.0.3'
```

## Rounded corners
It's possible to set rounded corners without, the algorithm is the same way as with other regular Views:

Create a rounded drawable, and set it as a background.

Then set up the clipping, so the BlurView doesn't draw outside the corners 
```Java
blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
blurView.setClipToOutline(true);
```
Related thread - https://github.com/Dimezis/BlurView/issues/37

## Why blurring on the main thread?
Because blurring on other threads would introduce 1-2 frames of latency.

## Comparing to other blurring libs
- The main advantage of BlurView over almost any other library is that it doesn't trigger redundant redraw.
- The BlurView never invalidates itself or other Views in the hierarchy and updates only when needed relying on just a Bitmap mutation, which is recorded on a hardware-accelerated canvas.
- It supports multiple BlurViews on the screen without triggering a draw loop.
- It uses optimized RenderScript Allocations on devices that require certain Allocation sizes, which greatly increases blur performance.
- It allows choosing a custom root view to take a snapshot from, which reduces the amount of drawing traversals and allows greater flexibility.
- Supports blurring of Dialogs (and Dialog's background)

Other libs:
- 🛑 [BlurKit](https://github.com/CameraKit/blurkit-android) - constantly invalidates itself
- 🛑 [RealtimeBlurView](https://github.com/mmin18/RealtimeBlurView) - constantly invalidates itself

License
-------

    Copyright 2022 Dmytro Saviuk

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
