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

       <!--Any child View here, TabLayout for example-->

  </eightbitlab.com.blurview.BlurView>
```

```Java
    final float radius = 16;

    final View decorView = getWindow().getDecorView();
    //Activity's root View. Can also be root View of your layout
    final View rootView = decorView.findViewById(android.R.id.content);
    //set background, if your root layout doesn't have one
    final Drawable windowBackground = decorView.getBackground();

    blurView.setupWith(rootView)
           .windowBackground(windowBackground)
           .blurAlgorithm(new RenderScriptBlur(this, true)) //Optional, enabled by default. User can have custom implementation
           .blurRadius(radius);
```

## Enable RenderScript support mode

```Groovy
 defaultConfig {
        renderscriptTargetApi 24
        renderscriptSupportModeEnabled true
  }
```

## Important
BlurView can be used only in a hardware-accelerated window.
Otherwise, blur will not be drawn. It will fallback to a regular FrameLayout drawing process.

## Performance
It takes 1-4ms on Nexus 5 and Nexus 4 to draw BlurView with the setup given in example project

## Gradle
```Groovy
compile 'com.eightbitlab:blurview:1.2.0'
```

License
-------

    Copyright 2016 Dmitry Saviuk

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
