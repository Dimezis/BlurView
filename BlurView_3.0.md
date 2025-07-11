> [!IMPORTANT]
> The BlurView version 3.0 significantly changes the API and the way it works.

## Why?

While I definitely appreciate that the old API was simpler and easier to use, it had a number of
issues:

- It used software rendering for view snapshotting. It comes with a whole bunch of caveats, some of
  which even I might not be aware of.
- Some things are simply impossible to render in software. For example, TextureView and everything TextureView-based.
  The old version didn't support this target.
- The software rendering also had a large performance impact. The view snapshot had to be redrawn
  every time something changed in the hierarchy,
  and the software drawing itself is usually much slower than hardware rendering. All this is added
  to the time that views spend in the regular hardware pass.
- It accidentally broke some things, for example the overstretch
  overscroll (https://github.com/Dimezis/BlurView/issues/234) and ripple effect on some Android
  versions (https://github.com/Dimezis/BlurView/issues/185)
- It incorrectly rendered some views (https://github.com/Dimezis/BlurView/issues/156)
- It interfered with Compose's recomposition (https://github.com/Dimezis/BlurView/issues/195)
- It crashed on rendering of Hardware Bitmaps

The new 3.0 version comes with a new API that's based on RenderNodes and RenderEffect (for API 31+
only, on older versions it falls back to the old code path).<br/>
The View snapshotting is now fully hardware-accelerated, it supports all of the previously
unsupported targets.<br/>
The View snapshotting has basically zero overhead. The BlurTarget records the snapshot on a
RenderNode, and then draws the same RenderNode on the system canvas.<br/>
The RenderNode snapshot is automatically updated whenever the View hierarchy changes, there's 0
additional`invalidate()` or `draw()` calls.<br/>
All this comes at the cost of a more complex API.

## Migration

Now you have to wrap the content you want to blur
into a `BlurTarget`, and pass it into the `setupWith()` method of the `BlurView`.<br/>
The BlurTarget may not contain a BlurView that targets the same BlurTarget.<br/>
The BlurTarget may contain other BlurTargets and BlurViews though.<br/>

## Scale factor

The scale factor was always used in BlurView to reduce the size of the View snapshot to improve the
blur performance at the cost of snapshot (and blur) quality/precision.<br/>
Right now the default scale factor is set to 4 down from 6 in the previous versions.<br/>
You can also now control it by passing it to `setupWith()` method.<br/>
On API <31 the scale factor is a key part to make the blur perform reasonably well, but on API 31+<br/> 
the RenderEffect already internally scales the snapshot when needed, so on newer APIs passing a<br/>
blur radius of 20 and scale factor of 3 is the same as passing a blur radius of 60 and scale factor of 1.<br/>

## Animation 

While the BlurView keeps honoring its position, scale, rotation transformations, you now have to
manually notify it about certain changes.<br/>
If you're animating the BlurView using `setTranslationX`, `setScaleX`, etc, you're fine and don't
have to do anything extra.<br/>
If you're animating it with `blurView.animate().translationX(...)...`, you have to attach an update
listener to the animator and call `blurView.notifyTranslationXChanged(...)` on every update.<br/>

Example:
```Java
int endY = 1000;
blurView.animate().translationY(endY).setUpdateListener(animation -> {
    // getAnimatedValue really returns just a fraction from 0 to 1
    blurView.notifyTranslationYChanged((Float) animation.getAnimatedValue() * endY);        
});
```

Also, you can't animate the `BlurTarget` with these property animators, but you can animate its
content to achieve the same effect.<br/>

## Bugs
It's a radical rewrite, so I expect some things to be broken. Please report any issues you
find.<br/>
