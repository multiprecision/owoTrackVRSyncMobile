package org.owoTrack;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import org.owoTrack.math.Quaternion;

public class SensorListener implements SensorEventListener {
    UdpPacketHandler udpClient;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelSensor;

    private Handler mHandler;

    SensorListener(SensorManager manager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        sensorManager = manager;


        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
                logger.update("Could not find any suitable rotation sensor!");
                throw new Exception("Your device does not have the required sensors and is unsupported.");
        }

        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelSensor == null)
            logger.update("Linear Acceleration sensor could not be found, this data will be unavailable.");

        udpClient = udpClient_v;
        udpClient.set_listener(this);
    }

    public void registerListeners() {
        mHandler = new Handler();
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
    }


    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] quaternion = new float[4];
            SensorManager.getQuaternionFromVector(quaternion, event.values);
            udpClient.sendRotationData(quaternion);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            udpClient.provide_accel(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //magnetometerAccuracy = accuracy;
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        //    System.out.println("@@@ Accuracy for " + sensor.getStringType() + " : " + String.valueOf(accuracy));
        //}

        //udpClient.provide_accuracy(accuracy);
    }
}
