package org.owoTrack;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;

public class RotationVectorSensorDataProvider implements SensorDataProvider {
    UdpPacketHandler udpClient;
    private SensorManager sensorManager;
    private Sensor rotationSensor;

    private Handler mHandler;

    RotationVectorSensorDataProvider(SensorManager manager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        sensorManager = manager;


        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
                logger.update("Could not find any suitable rotation sensor!");
                throw new Exception("Your device does not have the required sensors and is unsupported.");
        }

        udpClient = udpClient_v;
    }

    @Override
    public void register() {
        mHandler = new Handler();
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
    }

    @Override
    public void unregister() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] quaternion = new float[4];
            SensorManager.getQuaternionFromVector(quaternion, event.values);
            udpClient.sendRotationData(quaternion);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
