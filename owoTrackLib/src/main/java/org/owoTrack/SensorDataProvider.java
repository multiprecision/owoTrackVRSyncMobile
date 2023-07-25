package org.owoTrack;

import android.hardware.SensorEventListener;

public interface SensorDataProvider extends SensorEventListener {
    void register();
    void unregister();
}
