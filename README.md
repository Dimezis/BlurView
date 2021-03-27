# BlurView

![alt tag](https://github.com/Dimezis/BlurView/blob/master/BlurScreenshot.png)

Dynamic iOS-like blur of underlying Views for Android. 
Includes library and small example project.

BlurView can be used as a regular FrameLayout. It blurs its underlying content and draws it as a background for its children.
BlurView redraws its blurred content when changes in view hierarchy are detected (draw() called). 
It honors its position and size changes, including view animation and property animation.

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
    //ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
    ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
    //Set drawable to draw in the beginning of each blurred frame (Optional). 
    //Can be used in case your layout has a lot of transparent space and your content
    //gets kinda lost after after blur is applied.
    Drawable windowBackground = decorView.getBackground();

    blurView.setupWith(rootView)
           .setFrameClearDrawable(windowBackground)
           .setBlurAlgorithm(new RenderScriptBlur(this))
           .setBlurRadius(radius)
           .setBlurAutoUpdate(true)
           .setHasFixedTransformationMatrix(true); // Or false if it's in a scrolling container or might be animated
```

Always try to choose the closest possible root layout to BlurView. This will greatly reduce the amount of work needed for creating View hierarchy snapshot.

You can use `setHasFixedTransformationMatrix(true)` in case if you are not animating your BlurView, or not putting it in the scrolling container, this might slightly improve the performance as BlurView won't have to recalculate its coordinates on each frame. 

DO NOT set `View.LAYER_TYPE_HARDWARE` or `View.LAYER_TYPE_SOFTWARE` on the BlurView.
It's not supported (even though it could be), because it wouldn't bring any performance benefits.

## Supporting API < 17
If you need to support API < 17, you can include

```Groovy
implementation 'com.eightbitlab:supportrenderscriptblur:1.0.2'
```

setup BlurView with

```Java
blurAlgorithm(new SupportRenderScriptBlur(this))
```

and enable RenderScript support mode

```Groovy
 defaultConfig {
        renderscriptTargetApi 28 //must match target sdk and build tools
        renderscriptSupportModeEnabled true
 }
```

## Important
BlurView can be used only in a hardware-accelerated window.
Otherwise, blur will not be drawn. It will fallback to a regular FrameLayout drawing process.

## Gradle
```Groovy
implementation 'com.eightbitlab:blurview:1.6.6'
```
Since JCenter is closing, consider using https://jitpack.io/ and release tags as a source of stable artifacts.
Soon the old artifacts won't be available.

## Why blurring on the main thread?
Because blurring on some other thread would introduce 1-2 frames latency.
Though this is possible and already done on the very old branch as an experiment (which should be rewritten from scratch TBH)

License
-------

    Copyright 2021 Dmitry Saviuk

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
