package tk.mific.ringseekbar.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import tk.mific.ringseekbar.RingSeekBar;

public class MainActivity extends Activity {
    private static final String TAG = "Ring";

    private RingSeekBar mRing;
    private TextView mValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRing = (RingSeekBar) findViewById(R.id.ring);
        mRing.setOnValueChangeListener(new RingSeekBar.OnValueChangeListener() {
            @Override
            public void onValueChanged(int value, boolean fromUser) {
                if (fromUser) {
                    Log.i(TAG, "val: " + value);
                }
                mValue.setText(String.valueOf(value));
            }

            @Override
            public void onValueChangedDone(int value, boolean fromUser) {

            }
        });

        mValue = (TextView) findViewById(R.id.tv_value);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(!item.isChecked());

        switch (item.getItemId()) {
            case R.id.menu_fade_enable:
                mRing.setIsFade(true);
                return true;
            case R.id.menu_fade_disable:
                mRing.setIsFade(false);
                return true;
            case R.id.menu_to_zero:
                mRing.setValue(0, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
