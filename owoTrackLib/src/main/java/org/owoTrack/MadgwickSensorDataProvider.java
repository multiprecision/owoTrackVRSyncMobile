package org.owoTrack;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.renderscript.Matrix3f;

import com.kircherelectronics.fsensor.observer.SensorSubject;
import com.kircherelectronics.fsensor.util.rotation.RotationUtil;

import org.apache.commons.math3.complex.Quaternion;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
 * Copyright 2018, Kircher Electronics, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


public class MadgwickSensorDataProvider implements SensorDataProvider {
    UdpPacketHandler udpClient;

    private SensorManager sensorManager;
    private Handler mHandler;

    private Sensor accelSensor;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private final MadgwickFilter madgwick;
    private float ax = Float.NaN, ay, az, gx, gy, gz, mx, my, mz;

    private boolean isInitialRotationSet = false;
    private long timestamp = 0;

    MadgwickSensorDataProvider(float beta, SensorManager sensorManager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        madgwick = new MadgwickFilter(beta);
        this.sensorManager = sensorManager;
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelSensor == null) logger.update("Linear Acceleration sensor could not be found, this data will be unavailable.");
        udpClient = udpClient_v;
    }

    @Override
    public void register() {
        mHandler = new Handler();
        sensorManager.registerListener(this, magneticFieldSensor, 10_000, mHandler);
        sensorManager.registerListener(this, gyroscopeSensor, 10_000, mHandler);
        sensorManager.registerListener(this, accelerometerSensor, 10_000, mHandler);
    }

    @Override
    public void unregister() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
            if (isInitialRotationSet) {
                float dt = (event.timestamp - timestamp) * 1e-9f;
                madgwick.update(gx, gy, gz, ax, ay, az, mx, my, mz, dt);
                udpClient.sendRotationData(madgwick.q0, madgwick.q1, madgwick.q2, madgwick.q3);
                timestamp = event.timestamp;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mx = event.values[0];
            my = event.values[1];
            mz = event.values[2];
            if (!isInitialRotationSet && !Float.isNaN(ax)) {
                Quaternion q = RotationUtil.getOrientationVectorFromAccelerationMagnetic(new float[]{ax, ay, az}, new float[]{mx, my, mz});
                madgwick.q0 = (float) q.getQ0();
                madgwick.q1 = (float) q.getQ1();
                madgwick.q2 = (float) q.getQ2();
                madgwick.q3 = (float) q.getQ3();
                timestamp = event.timestamp;
                isInitialRotationSet = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
