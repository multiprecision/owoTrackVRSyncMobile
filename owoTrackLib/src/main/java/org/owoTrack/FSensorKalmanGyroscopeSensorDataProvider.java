package org.owoTrack;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.kircherelectronics.fsensor.filter.gyroscope.fusion.OrientationFused;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.complementary.OrientationFusedComplementary;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.OrientationFusedKalman;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationKalmanFilter;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationMeasurementModel;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationProcessModel;
import com.kircherelectronics.fsensor.observer.SensorSubject;
import com.kircherelectronics.fsensor.sensor.FSensor;
import com.kircherelectronics.fsensor.sensor.gyroscope.KalmanGyroscopeSensor;
import com.kircherelectronics.fsensor.util.angle.AngleUtils;
import com.kircherelectronics.fsensor.util.rotation.RotationUtil;

import org.apache.commons.math3.complex.Quaternion;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

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


public class FSensorKalmanGyroscopeSensorDataProvider implements SensorDataProvider {
    UdpPacketHandler udpClient;

    private SensorManager sensorManager;
    private Handler mHandler;

    private KalmanGyroscopeSensor1 fSensor;

    private SensorSubject.SensorObserver sensorObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            udpClient.sendRotationData(values);
        }
    };

    FSensorKalmanGyroscopeSensorDataProvider(Context context1, SensorManager sensorManager, UdpPacketHandler udpClient_v, AppStatus logger) throws Exception {
        fSensor = new KalmanGyroscopeSensor1(context1);
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
