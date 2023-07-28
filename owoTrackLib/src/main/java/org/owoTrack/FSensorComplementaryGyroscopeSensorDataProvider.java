package org.owoTrack;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;

import com.kircherelectronics.fsensor.observer.SensorSubject;

public class FSensorComplementaryGyroscopeSensorDataProvider implements SensorDataProvider {
    UdpPacketHandler udpClient;

    private SensorManager sensorManager;
    private Handler mHandler;

    private ComplementaryGyroscopeSensor1 fSensor;

    private SensorSubject.SensorObserver sensorObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            udpClient.sendRotationData(values);
        }
    };

    FSensorComplementaryGyroscopeSensorDataProvider(Context context1, SensorManager sensorManager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        fSensor = new ComplementaryGyroscopeSensor1(context1);
        //fSensor.setFSensorComplimentaryTimeConstant(0.5f);
        this.sensorManager = sensorManager;
        udpClient = udpClient_v;
    }

    @Override
    public void register() {
        mHandler = new Handler();
        fSensor.register(sensorObserver);
        fSensor.start();
    }

    @Override
    public void unregister() {
        sensorManager.unregisterListener(this);
        fSensor.unregister(sensorObserver);
        fSensor.stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}

