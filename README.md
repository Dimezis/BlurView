[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://vshymanskyy.github.io/StandWithUkraine)

# BlurView

<a href="url"><img src="https://github.com/user-attachments/assets/5abb1034-021b-4dfb-ad1b-3136a2a00a02" width="432" ></a>

Dynamic iOS-like blur for Android Views. Includes a library and a small example project.

BlurView can be used as a regular FrameLayout. It blurs its underlying content and draws it as a
background for its children. The children of the BlurView are not blurred. BlurView updates its
blurred content when changes in the view hierarchy are detected. It honors its position
and size changes, including view animation and property animation.

> [!IMPORTANT]
> Version 3.0 info, key changes, migration steps, and what you need to know is [here](BlurView_3.0.md).<br/>
> Also, the code path on API 31+ is now completely different from API < 31, so keep in mind to test both.

## How to use
```XML
    <eightbitlab.com.blurview.BlurView
      android:id="@+id/blurView"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:blurOverlayColor="@color/colorOverlay">
    
       <!--Any child View here, TabLayout for example. This View will NOT be blurred -->
    
    </eightbitlab.com.blurview.BlurView>

    <!--This is the content to be blurred by the BlurView. 
    It will render normally, and BlurView will use its snapshot for blurring-->
    <eightbitlab.com.blurview.BlurTarget
        android:id="@+id/target"
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        
        <!--Your main content here-->

    </eightbitlab.com.blurview.BlurTarget>
```

```Java
    float radius = 20f;

    View decorView = getWindow().getDecorView();
    // A view hierarchy you want blur. The BlurTarget can't include the BlurView that targets it.
    BlurTarget target = findViewById(R.id.target);
    
    // Optional:
    // Set the drawable to draw in the beginning of each blurred frame.
    // Can be used in case your layout has a lot of transparent space and your content
    // gets a low alpha value after blur is applied.
    Drawable windowBackground = decorView.getBackground();

    // Optionally pass a custom BlurAlgorithm and scale factor as additional parameters.
    // You might want to set a smaller scale factor on API 31+ to have a more precise blur with less flickering.
    blurView.setupWith(target) 
           .setFrameClearDrawable(windowBackground) // Optional. Useful when your root has a lot of transparent background, which results in semi-transparent blurred content. This will make the background opaque
           .setBlurRadius(radius)
```

## SurfaceView, TextureView, VideoView, MapFragment, GLSurfaceView, etc
TextureView can be blurred only on API 31+. Everything else (which is SurfaceView-based) can't be blurred, unfortunately.

## Gradle

Use Jitpack https://jitpack.io/#Dimezis/BlurView and release tags as the source of stable
artifacts.
```Groovy
implementation 'com.github.Dimezis:BlurView:version-3.0.0'
```

## Rounded corners
It's possible to set rounded corners without any custom API, the algorithm is the same as with other regular View:

Create a rounded drawable, and set it as a background.

Then set up the clipping, so the BlurView doesn't draw outside the corners 
```Java
blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
blurView.setClipToOutline(true);
```
Related thread - https://github.com/Dimezis/BlurView/issues/37

## Why blurring on the main thread?
Because blurring on other threads would introduce 1-2 frames of latency.
On API 31+ the blur is done on the system Render Thread.

## Compared to other blurring libs
- BlurView and Haze for Compose are the only libraries that leverage hardware acceleration for View snapshotting and have near zero overhead of snapshotting.
- Supports TextureView blur on API 31+.
- The BlurView never invalidates itself or other Views in the hierarchy and updates only when needed.
- It supports multiple BlurViews on the screen without triggering a draw loop.
- On API < 31 it uses optimized RenderScript Allocations on devices that require certain Allocation sizes, which greatly increases blur performance.
- Supports blurring of Dialogs (and Dialog's background)

Other libs:
- 🛑 [BlurKit](https://github.com/CameraKit/blurkit-android) - constantly invalidates itself
- 🛑 [RealtimeBlurView](https://github.com/mmin18/RealtimeBlurView) - constantly invalidates itself

License
-------

    Copyright 2025 Dmytro Saviuk

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
