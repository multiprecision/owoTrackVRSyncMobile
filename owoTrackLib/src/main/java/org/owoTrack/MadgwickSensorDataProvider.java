package org.owoTrack;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;

import com.kircherelectronics.fsensor.observer.SensorSubject;

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

    private volatile ScheduledFuture<?> self;
    private SensorSubject.SensorObserver sensorObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            udpClient.sendRotationData(values);
        }
    };

    private MadgwickFilter madgwick = new MadgwickFilter(0.01f, 0.00001f);
    private float ax, ay, az, gx, gy, gz, mx, my, mz;

    MadgwickSensorDataProvider(Context context1, SensorManager sensorManager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        //fSensor.setFSensorComplimentaryTimeConstant(0.5f);
        this.sensorManager = sensorManager;
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelSensor == null) logger.update("Linear Acceleration sensor could not be found, this data will be unavailable.");
        udpClient = udpClient_v;
    }

    @Override
    public void register() {
        mHandler = new Handler();
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(1);

        self = scheduler.scheduleWithFixedDelay(() -> {
            madgwick.update(gx, gy, gz, ax, ay, az, mx, my, mz);
            float[] quaternion = madgwick.getQuaternion();
            udpClient.sendRotationData(quaternion);
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unregister() {
        sensorManager.unregisterListener(this);
        self.cancel(false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            udpClient.sendAcceleration(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mx = event.values[0];
            my = event.values[1];
            mz = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}

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

