## RingSeekBar

Smoothly rotating seekBar

## Usage

### in layout file:

```xml
    <tk.mific.ringseekbar.RingSeekBar
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/ring"
        android:layout_gravity="center_horizontal"
        app:rsb_srcRing="@drawable/ring"
        app:rsb_value="0"
        app:rsb_maxValue="360"
        app:rsb_minValue="-360"
        app:rsb_valueRatio="1.0"
        app:rsb_velocityRatio="7"
        app:rsb_flingSensitivity="10.0"
        app:rsb_flingDuration="1300"
        app:rsb_active="true"
        app:rsb_fade_in="true"
        app:rsb_fade_duration="1000"
        app:rsb_fade_from="0.1"
        app:rsb_fade_to="1.0"
        app:rsb_thickness="40dp"/>
```

### in code

```java
    mRing = (RingSeekBar) findViewById(R.id.ring);
    mRing.setOnValueChangeListener(new RingSeekBar.OnValueChangeListener() {
        @Override
        public void onValueChanged(int value, boolean fromUser) {
            //change value event
            if (fromUser) {
                //chenge value event caused by the user
                Log.i(TAG, "val: " + value);
            }
            mValue.setText(String.valueOf(value));
        }

        @Override
        public void onValueChangedDone(int value, boolean fromUser) {
            //touch up event
        }
    });
```

## Attributes
*name*|*description*|*type*
------|-------------|------
rsb_srcRing|Drawable|@drawable
rsb_value  |Current value|int
rsb_maxValue|Maximum value|int
rsb_minValue|Minimum value|int
rsb_valueRatio|Ratio angle to value|float 
rsb_velocityRatio|Ratio of velocity|float
rsb_flingSensitivity|Sensitivity of fling event|float
rsb_flingDuration|Duration of fling animation|int
rsb_active|Active|boolean
rsb_fade_in|Enable fade animation|boolean
rsb_fade_duration|Duration of fade animation|boolean
rsb_fade_from|Start alpha for fade animation|float
rsb_fade_to|End alpha for fade animation|float
rsb_thickness|Thickness of touch area|dp

## License


```
Copyright 2015 Sergey Aleynik 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```