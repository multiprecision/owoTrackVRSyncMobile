package org.owoTrack.Mobile;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static Map<String, Boolean> sensorExist = new HashMap<>();
    public static NavController contr;
    private static String missingSensorMessage = "";

    public static boolean getSensorExists(String type) {
        return Boolean.TRUE.equals(sensorExist.get(type));
    }

    public static boolean missingRequiredSensor() {
        return !getSensorExists("TYPE_ROTATION_VECTOR") && !getSensorExists("TYPE_GAME_ROTATION_VECTOR");
    }

    public static String getSensorText() {
        return missingSensorMessage;
    }

    private void fillSensorArray() {
        SensorManager man = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorExist.put("TYPE_ROTATION_VECTOR", man.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);
        sensorExist.put("TYPE_GAME_ROTATION_VECTOR", man.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null);
        sensorExist.put("TYPE_LINEAR_ACCELERATION", man.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null);
        sensorExist.put("TYPE_GYROSCOPE", man.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
        sensorExist.put("TYPE_ACCELEROMETER", man.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
        sensorExist.put("TYPE_MAGNETIC_FIELD", man.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);

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