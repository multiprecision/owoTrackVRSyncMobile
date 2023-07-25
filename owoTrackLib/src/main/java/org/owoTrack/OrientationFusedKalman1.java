package org.owoTrack;

import android.util.Log;

import com.kircherelectronics.fsensor.filter.gyroscope.fusion.OrientationFused;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.complementary.OrientationFusedComplementary;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationKalmanFilter;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationMeasurementModel;
import com.kircherelectronics.fsensor.filter.gyroscope.fusion.kalman.filter.RotationProcessModel;
import com.kircherelectronics.fsensor.util.rotation.RotationUtil;

import org.apache.commons.math3.complex.Quaternion;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of a Kalman fusedOrientation based orientation sensor fusion.
 * * <p>
 * The fusedOrientation attempts to fuse magnetometer, gravity and gyroscope
 * sensors together to produce an accurate measurement of the rotation of the
 * device.
 * <p>
 * The magnetometer and acceleration sensors are used to determine one of the
 * two orientation estimations of the device. This measurement is subject to the
 * constraint that the device must not be accelerating and hard and soft-iron
 * distortions are not present in the local magnetic field.
 * <p>
 * The gyroscope is used to determine the second of two orientation estimations
 * of the device. The gyroscope can have a shorter response time and is not
 * effected by linear acceleration or magnetic field distortions, however it
 * experiences drift and has to be compensated periodically by the
 * acceleration/magnetic sensors to remain accurate.
 * <p>
 * Quaternions are used to integrate the measurements of the gyroscope and apply
 * the rotations to each sensors measurements via Kalman fusedOrientation. This the
 * ideal method because quaternions are not subject to many of the singularties
 * of rotation matrices, such as gimbal lock.
 * <p>
 * Created by kaleb on 7/6/17.
 */

public class OrientationFusedKalman1 extends OrientationFused {

    private static final String TAG = OrientationFusedComplementary.class.getSimpleName();

    private final RotationKalmanFilter kalmanFilter;
    private final AtomicBoolean run;
    private volatile float dT;
    private volatile float[] output = new float[3];
    private Thread thread;

    private volatile Quaternion rotationVectorAccelerationMagnetic;
    private final double[] vectorGyroscope = new double[4];
    private final double[] vectorAccelerationMagnetic = new double[4];

    public OrientationFusedKalman1() {
        this(DEFAULT_TIME_CONSTANT);
    }

    public OrientationFusedKalman1(float timeConstant) {
        super(timeConstant);
        run = new AtomicBoolean(false);
        kalmanFilter = new RotationKalmanFilter(new RotationProcessModel(), new RotationMeasurementModel());
    }

    public void startFusion() {
        if (!run.get() && thread == null) {
            run.set(true);

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (run.get() && !Thread.interrupted()) {

                        output = calculate();

                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Kalman Thread", e);
                            Thread.currentThread().interrupt();
                        }
                    }

                    Thread.currentThread().interrupt();
                }
            });

            thread.start();
        }
    }

    public void stopFusion() {
        if (run.get() && thread != null) {
            run.set(false);
            thread.interrupt();
            thread = null;
        }
    }

    public float[] getOutput() {
        return output;
    }

    /**
     * Calculate the fused orientation of the device.
     * <p>
     * Rotation is positive in the counterclockwise direction (right-hand rule). That is, an observer looking from some positive location on the x, y, or z axis at
     * a device positioned on the origin would report positive rotation if the device appeared to be rotating counter clockwise. Note that this is the
     * standard mathematical definition of positive rotation and does not agree with the aerospace definition of roll.
     * <p>
     * See: https://source.android.com/devices/sensors/sensor-types#rotation_vector
     * <p>
     * Returns a vector of size 3 ordered as:
     * [0]X points east and is tangential to the ground.
     * [1]Y points north and is tangential to the ground.
     * [2]Z points towards the sky and is perpendicular to the ground.
     *
     * @return An orientation vector -> @link SensorManager#getOrientation(float[], float[])}
     */
    private float[] calculate() {
        if (rotationVector != null && rotationVectorAccelerationMagnetic != null && dT != 0) {
            vectorGyroscope[0] = (float) rotationVector.getVectorPart()[0];
            vectorGyroscope[1] = (float) rotationVector.getVectorPart()[1];
            vectorGyroscope[2] = (float) rotationVector.getVectorPart()[2];
            vectorGyroscope[3] = (float) rotationVector.getScalarPart();

            vectorAccelerationMagnetic[0] = (float) rotationVectorAccelerationMagnetic.getVectorPart()[0];
            vectorAccelerationMagnetic[1] = (float) rotationVectorAccelerationMagnetic.getVectorPart()[1];
            vectorAccelerationMagnetic[2] = (float) rotationVectorAccelerationMagnetic.getVectorPart()[2];
            vectorAccelerationMagnetic[3] = (float) rotationVectorAccelerationMagnetic.getScalarPart();

            // Apply the Kalman fusedOrientation... Note that the prediction and correction
            // inputs could be swapped, but the fusedOrientation is much more stable in this
            // configuration.
            kalmanFilter.predict(vectorGyroscope);
            kalmanFilter.correct(vectorAccelerationMagnetic);

            // rotation estimation.
            Quaternion result = new Quaternion(kalmanFilter.getStateEstimation()[3], Arrays.copyOfRange(kalmanFilter.getStateEstimation(), 0, 3));

            float[] results = new float[4];
            results[0] = (float) result.getQ0();
            results[1] = (float) result.getQ1();
            results[2] = (float) result.getQ2();
            results[3] = (float) result.getQ3();
            output = results;
        }

        return output;
    }

    /**
     * Calculate the fused orientation of the device.
     *
     * @param gyroscope    the gyroscope measurements.
     * @param timestamp    the gyroscope timestamp
     * @param acceleration the acceleration measurements
     * @param magnetic     the magnetic measurements
     * @return the fused orientation estimation.
     */
    public float[] calculateFusedOrientation(float[] gyroscope, long timestamp, float[] acceleration, float[] magnetic) {
        if (isBaseOrientationSet()) {
            if (this.timestamp != 0) {
                dT = (timestamp - this.timestamp) * NS2S;

                rotationVectorAccelerationMagnetic = RotationUtil.getOrientationVectorFromAccelerationMagnetic(acceleration, magnetic);
                rotationVector = RotationUtil.integrateGyroscopeRotation(rotationVector, gyroscope, dT, EPSILON);
            }
            this.timestamp = timestamp;

            return output;
        } else {
            throw new IllegalStateException("You must call setBaseOrientation() before calling calculateFusedOrientation()!");
        }
    }
}
