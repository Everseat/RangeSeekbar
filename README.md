# RangeSeekbar
A seekbar widget with a configurable min/max range

# Install
```groovy
allprojects {
 repositories {
    ...
    maven { url "https://jitpack.io" }
 }
}
```

```
compile 'com.github.everseat:RangeSeekbar:0.1'
```

# Usage

```xml
<com.everseat.rangeseekbar.RangeSeekbar
	android:id="@+id/range_seekbar"
	android:layout_width="match_parent"
	android:layout_height="wrap_content"
  app:trackColor="#000"
  app:trackFillColor="#FFF"/>
```

```java
RangeSeekbar seekbar = findViewById(R.id.range_seekbar);
seekbar.setTrackColor(Color.BLACK);
seekbar.setTrackFillColor(Color.WHITE);
```
