The BlurView version 3.0 significantly changes the API and the way it works.

## Why?

While I definitely appreciate that the old API was simpler and easier to use, it had a number of
issues:

- It used software rendering for view snapshotting. It comes with a whole bunch of caveats, some of
  which even I might not be aware of.
- Some things are simply impossible to render in software, for example SurfaceView, TextureView,
  GLSurfaceView, VideoView, Google Maps, game engine views, etc.
  The old version didn't support blurring of these targets.
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
only, on older versions it falls back to the old code path).
The View snapshotting is now fully hardware-accelerated, it supports all of the previously
unsupported targets.
The View snapshotting has basically zero overhead. The BlurTarget records the snapshot on a
RenderNode, and then draws the same RenderNode on the system canvas.
The RenderNode snapshot is automatically updated whenever the View hierarchy changes, there's 0
additional
`invalidate()` or `draw()` calls.
All this comes at the cost of a more complex API.

## Migration

Now you have to wrap the content you want to blur
into a `BlurTarget`, and pass it into the `setupWith()` method of the `BlurView`.
The BlurTarget may not contain a BlurView that targets the same BlurTarget.

While the BlurView keeps honoring its position, scale, rotation transformations, you now have to
manually notify it about certain changes.
If you are animating the BlurView using `setTranslationX`, `setScaleX`, etc, you're fine and don't
have to do anything extra.
If you're animating it with `blurView.animate().translationX(...)...`, you have to attach an update
listener to the animator and call `blurView.notifyTranslationXChanged(...)` on every update.

Also, you can't animate the `BlurTarget` with these property animators, but you can animate its
content to achieve the same effect.

It's a radical rewrite, so I expect some things to be broken. Please report any issues you find.

## Scale factor

The scale factor was always used in BlurView to reduce the size of the View snapshot to improve the
blur
performance at the cost of snapshot (and blur) quality/precision.
Right now the default scale factor is set to 4 down from 6 in the previous versions.
You can also now control it by passing it to `setupWith()` method.
On API <31 the scale factor is a key part to make the blur perform reasonably well, but on newer
versions the performance difference is not as immediately noticeable, although I haven't measured
it :) 