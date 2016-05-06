# BlurView

![alt tag](https://github.com/Dimezis/BlurView/blob/master/BlurScreenshot.png)

Dynamic iOS-like blur of underlying Views for Android. 
Includes library and small example project.

BlurView can be used as a regular FrameLayout. It blurs its underlying content and draws it as a background for its children.
BlurView redraws its blurred content when changes in view hierarchy are detected (draw() called). 
It honors its position and size changes, including view animation and property animation.

## How to use:
```XML
  <eightbitlab.com.blurview.BlurView
      android:id="@+id/blurView"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:overlayColor="@color/colorOverlay">

       <!--Any child View here, TabLayout for example-->

  </eightbitlab.com.blurview.BlurView>
```

```Java
    final int radius = 16;

    final View decorView = getWindow().getDecorView();
    //Activity's root View. Can also be root View of your layout
    final View rootView = decorView.findViewById(android.R.id.content);
    //set background, if your root layout doesn't have one
    final Drawable windowBackground = decorView.getBackground();

    blurView.setupWith(rootView)
           .windowBackground(windowBackground)
           .blurAlgorithm(new RenderScriptBlur(this, true)) //Preferable algorithm, needs RenderScript support mode enabled
           .blurRadius(radius);
```

## Enable RenderScript support mode:

```Groovy
 defaultConfig {
        renderscriptTargetApi 19
        renderscriptSupportModeEnabled true
  }
```

## Perfomance
It takes 1-4ms on Nexus 5 and Nexus 4 to draw BlurView with the setup given in example project
