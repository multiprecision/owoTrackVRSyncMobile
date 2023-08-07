package org.owoTrack.Mobile;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.owoTrack.AutoDiscoverer;
import org.owoTrack.HandshakeHandler;
import org.owoTrack.Mobile.ui.ConnectFragment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static Map<String, Boolean> sensorExist = new LinkedHashMap<>();
    public static Map<String, Integer> sensorMinDelay = new LinkedHashMap<>();
    public static NavController contr;
    private static String missingSensorMessage = "";

    public static boolean getSensorExists(String type) {
        return Boolean.TRUE.equals(sensorExist.get(type));
    }

    public static Integer getSensorMinDelay(String type) {
        return sensorMinDelay.get(type);
    }

    public static boolean missingRequiredSensor() {
        return !getSensorExists("TYPE_ROTATION_VECTOR") && !getSensorExists("TYPE_GAME_ROTATION_VECTOR");
    }

    public static String getSensorText() {
        return missingSensorMessage;
    }

    private void fillSensorArray() {
        SensorManager man = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Sensor s0 = man.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorExist.put("TYPE_ROTATION_VECTOR", s0 != null);
        if (s0 != null)
            sensorMinDelay.put("TYPE_ROTATION_VECTOR", s0.getMinDelay());

        Sensor s1 = man.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorExist.put("TYPE_GAME_ROTATION_VECTOR", s1 != null);
        if (s1 != null)
            sensorMinDelay.put("TYPE_GAME_ROTATION_VECTOR", s1.getMinDelay());

        Sensor s2 = man.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorExist.put("TYPE_LINEAR_ACCELERATION", s2 != null);
        if (s2 != null)
            sensorMinDelay.put("TYPE_LINEAR_ACCELERATION", s2.getMinDelay());

        Sensor s3 = man.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorExist.put("TYPE_GYROSCOPE", s3 != null);
        if (s3 != null)
            sensorMinDelay.put("TYPE_GYROSCOPE", s3.getMinDelay());

        Sensor s4 = man.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorExist.put("TYPE_ACCELEROMETER", s4 != null);
        if (s4 != null)
            sensorMinDelay.put("TYPE_ACCELEROMETER", s4.getMinDelay());

        Sensor s5 = man.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorExist.put("TYPE_MAGNETIC_FIELD", s5 != null);
        if (s5 != null)
            sensorMinDelay.put("TYPE_MAGNETIC_FIELD", s5.getMinDelay());

        missingSensorMessage = "";

        if (missingRequiredSensor()) {
            missingSensorMessage = getString(R.string.sensors_missing_all);
        }

    }

    private void ensureUUIDSet() {
        SharedPreferences prefs = getSharedPreferences("FakeMAC", Context.MODE_PRIVATE);

        long val = -1;
        if (!prefs.contains("FakeMACValue")) {
            SharedPreferences.Editor editor = prefs.edit();
            val = (new java.util.Random()).nextLong();
            editor.putLong("FakeMACValue", val);
            editor.apply();
        } else {
            val = prefs.getLong("FakeMACValue", 1);
        }

        HandshakeHandler.setMac(val);
    }

    private void connect(String ip, int port) {
        SharedPreferences prefs = ConnectFragment.getSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("ip_address", ip);
        editor.putInt("port", port);

        editor.apply();


        contr.navigate(R.id.connectFragment);

    }

    private void runDiscovery() {
        if (missingRequiredSensor()) return;
        if (!AutoDiscoverer.discoveryStillNecessary) return;

        try {
            AutoDiscoverer disc = new AutoDiscoverer(this, this::connect);
            Thread thrd = new Thread(disc::try_discover);
            thrd.start();
        } catch (OutOfMemoryError ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ensureUUIDSet();
        fillSensorArray();

        setContentView(R.layout.activity_main);

        contr = Navigation.findNavController(this, R.id.fragment);

        BottomNavigationView nav = findViewById(R.id.nav_view);

        NavigationUI.setupWithNavController(nav, contr);

        runDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                } else {
                    Toast.makeText(this, "Notification permission for foreground service was not granted.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}