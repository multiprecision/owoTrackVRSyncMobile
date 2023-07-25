package org.owoTrack;

import com.kircherelectronics.fsensor.util.angle.AngleUtils;
import com.kircherelectronics.fsensor.util.rotation.RotationUtil;

public class OrientationFusedComplementary1 extends OrientationFused1 {

    private static final String TAG = OrientationFusedComplementary1.class.getSimpleName();

    /**
     * Initialize a singleton instance.
     */
    public OrientationFusedComplementary1() {
        this(DEFAULT_TIME_CONSTANT);
    }

    public OrientationFusedComplementary1(float timeConstant) {
        super(timeConstant);
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
     * @param gyroscope the gyroscope measurements.
     * @param timestamp the gyroscope timestamp
     * @return An orientation vector -> @link SensorManager#getOrientation(float[], float[])}
     */
    public float[] calculateFusedOrientation(float[] gyroscope, long timestamp, float[] acceleration, float[] magnetic) {
        if (isBaseOrientationSet()) {
            if (this.timestamp != 0) {
                final float dT = (timestamp - this.timestamp) * NS2S;

                float alpha = timeConstant / (timeConstant + dT);
                float oneMinusAlpha = (1.0f - alpha);

                org.apache.commons.math3.complex.Quaternion rotationVectorAccelerationMagnetic = RotationUtil.getOrientationVectorFromAccelerationMagnetic(acceleration, magnetic);

                if (rotationVectorAccelerationMagnetic != null) {

                    rotationVector = RotationUtil.integrateGyroscopeRotation(rotationVector, gyroscope, dT, EPSILON);

                    // Apply the complementary fusedOrientation. // We multiply each rotation by their
                    // coefficients (scalar matrices)...
                    org.apache.commons.math3.complex.Quaternion scaledRotationVectorAccelerationMagnetic = rotationVectorAccelerationMagnetic.multiply(oneMinusAlpha);

                    // Scale our quaternion for the gyroscope
                    org.apache.commons.math3.complex.Quaternion scaledRotationVectorGyroscope = rotationVector.multiply(alpha);

                    //...and then add the two quaternions together.
                    // output[0] = alpha * output[0] + (1 - alpha) * input[0];
                    org.apache.commons.math3.complex.Quaternion result = scaledRotationVectorGyroscope.add(scaledRotationVectorAccelerationMagnetic);

                    float[] results = new float[4];
                    results[0] = (float) result.getQ0();
                    results[1] = (float) result.getQ1();
                    results[2] = (float) result.getQ2();
                    results[3] = (float) result.getQ3();
                    output = results;
                }
            }

            this.timestamp = timestamp;
            return output;
        } else {
            throw new IllegalStateException("You must call setBaseOrientation() before calling calculateFusedOrientation()!");
        }
    }
}

