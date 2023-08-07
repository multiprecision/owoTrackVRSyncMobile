package org.owoTrack.Wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.owoTrack.HandshakeHandler;
import org.owoTrack.TrackingService;
import org.owoTrack.Wear.databinding.ActivityMainwearBinding;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainWear extends Activity {

    boolean norm_rotation_exists;
    boolean connecting;
    boolean connected;
    boolean isAutoDiscover = true;
    TrackingService service_v = null;
    private ActivityMainwearBinding binding;
    private TextView debugText;
    private boolean dead_no_sensors = false;
    private Lock connecting_lock = new ReentrantLock();

    ;
    private ServiceConnection trackingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.TrackingBinder binderBridge = (TrackingService.TrackingBinder) service;
            service_v = binderBridge.getService();

            onConnectionStatus(service_v.is_running());

            onSetStatus(service_v.getLog());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service_v = null;

            onConnectionStatus(false);
        }
    };
    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "info-log":
                    onConnectionStatus(true);
                    String data = intent.getStringExtra("message");
                    onSetStatus(data);
                    return;
                case "disconnect":
                    onConnectionStatus(false);
                    return;
                case "reconnect-service":
                    doBinding(false);
                    doBinding(true);
                    return;

            }
        }
    };
    private int debounce_wifi = 0;
    private boolean wifi_acquired = true;

    public static SharedPreferences getSharedPreferences(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    private void onSetStatus(String to) {
        ;
        if (to.contains("Service not start")) return;

        String[] lines = to.split("\n");

        this.runOnUiThread(() -> {
            debugText.setText(lines[lines.length - 1]);
        });
    }

    private void onSetStatus(int rid) {
        ;
        onSetStatus(getString(rid));
    }

    private void onConnectionStatus(boolean to) {
        setConnectedStatus(connecting, to);
    }

    private void updateSensorStatus() {
        SensorManager man = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        norm_rotation_exists = (man.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);

        this.runOnUiThread(() -> {
            dead_no_sensors = !norm_rotation_exists;
            if (dead_no_sensors) {
                binding.yesSensorsLayout.setVisibility(View.GONE);
                binding.noSensorsLayout.setVisibility(View.VISIBLE);
            } else {
                binding.yesSensorsLayout.setVisibility(View.VISIBLE);
                binding.noSensorsLayout.setVisibility(View.GONE);
            }
        });
    }

    public SharedPreferences getSharedPreferences() {
        return getSharedPreferences(this);
    }

    private void onAutodiscoverChanged(View view) {
        binding.manualConnect.setVisibility(binding.autodiscover.isChecked() ? View.GONE : View.VISIBLE);
        isAutoDiscover = binding.autodiscover.isChecked();
    }

    private Pair<String, Integer> getIpPort() {
        String filtered_ip = String.valueOf(binding.editIpAddr.getText()).replaceAll("[^0-9\\.]", "");
        int port = 6969;
        try {
            port = Integer.parseInt(String.valueOf(binding.editPort.getText()));
        } catch (NumberFormatException ignored) {
        }

        int finalPort = port;
        this.runOnUiThread(() -> {
            binding.editIpAddr.setText(filtered_ip);
            binding.editPort.setText(String.valueOf(finalPort));
        });
        return new Pair<String, Integer>(filtered_ip, port);
    }

    private void load_prefs() {
        SharedPreferences prefs = getSharedPreferences();

        this.runOnUiThread(() -> {
            binding.editIpAddr.setText(prefs.getString("ip", ""));
            binding.editPort.setText(String.valueOf(prefs.getInt("port", 6969)));
            binding.autodiscover.setChecked(prefs.getBoolean("autodiscover", true));
        });
    }

    private void save_prefs() {
        SharedPreferences prefs = getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("ip", String.valueOf(binding.editIpAddr.getText()));
        try {
            editor.putInt("port", Integer.parseInt(String.valueOf(binding.editPort.getText())));
        } catch (NumberFormatException ignored) {
        }

        editor.putBoolean("autodiscover", binding.autodiscover.isChecked());

        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainwearBinding.inflate(getLayoutInflater());

        updateSensorStatus();

        binding.connectButton.setOnClickListener(this::connectToWifiAndRun);
        binding.autodiscover.setOnClickListener(this::onAutodiscoverChanged);

        debugText = binding.debugText;

        setContentView(binding.getRoot());

        setConnectedStatus(false, false);

        doBinding(true);
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, new IntentFilter("info-log"));
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, new IntentFilter("disconnect"));
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, new IntentFilter("reconnect-service"));

        load_prefs();
        onAutodiscoverChanged(null);

        ensureUUIDSet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        save_prefs();

        doBinding(false);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver);
    }

    private void setConnectedStatus(boolean connecting, boolean connected) {
        this.connecting = connecting;
        this.connected = connected;

        this.runOnUiThread(() -> {
            binding.spinner.setVisibility((connecting && !connected) ? View.VISIBLE : View.GONE);
            binding.connectButton.setVisibility((!connecting) ? View.VISIBLE : View.GONE);
            binding.connectButton.setEnabled(!connecting);

            binding.connectButton.setText(connected ? R.string.disconnect : R.string.connect);
        });
    }


    private void doBinding(boolean is_bound) {
        if (is_bound) {
            Intent intent = new Intent(this, TrackingService.class);
            this.bindService(intent, trackingConnection, BIND_AUTO_CREATE);
        } else {
            this.unbindService(trackingConnection);
        }
    }

    private void connect(String ip, int port) {
        onConnectionStatus(true);

        Intent mainIntent = new Intent(this, TrackingService.class);
        mainIntent.putExtra("ipAddrTxt", ip);
        mainIntent.putExtra("port_no", port);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(mainIntent);
        } else {
            this.startService(mainIntent);
        }
    }

    private void ensureUUIDSet() {
        SharedPreferences prefs = getSharedPreferences("FakeMACWear", Context.MODE_PRIVATE);

        long val = -1;
        if (!prefs.contains("FakeMACValueWear")) {
            SharedPreferences.Editor editor = prefs.edit();
            val = (new java.util.Random()).nextLong();
            editor.putLong("FakeMACValueWear", val);
            editor.apply();
        } else {
            val = prefs.getLong("FakeMACValueWear", 1);
        }

        HandshakeHandler.setMac(val);
    }

    private void runConnectionProcedure() {
        if (!connecting_lock.tryLock()) return;

        boolean server_found = false;
        try {
            onSetStatus(R.string.searching);

            if (isAutoDiscover) {
                this.connect("255.255.255.255", 6969);
            } else {
                Pair<String, Integer> ipPort = getIpPort();
                if (ipPort.first.length() < 3) {
                    onSetStatus("Please enter valid IP");
                    return;
                }

                server_found = true;
                this.connect(ipPort.first, ipPort.second);
            }
        } finally {
            connecting_lock.unlock();
            boolean finalServer_found = server_found;
            this.runOnUiThread(() -> {
                setConnectedStatus(false, finalServer_found);
            });
        }
    }

    private void connectToWifiAndRun(View view) {
        if (dead_no_sensors) return;

        if ((service_v != null) && (service_v.is_running())) {
            onSetStatus("Killing service...");
            Intent intent = new Intent("kill_service");
            this.sendBroadcast(intent);

            setConnectedStatus(false, false);
            return;
        }

        if (debounce_wifi > 0 && !wifi_acquired) {
            this.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
        }


        debounce_wifi++;
        int curr_deb = debounce_wifi;
        wifi_acquired = false;


        onSetStatus("Awaiting WiFi network...");

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            public void onAvailable(Network network) {
                if (curr_deb != debounce_wifi) return;
                wifi_acquired = true;

                super.onAvailable(network);

                // The Wi-Fi network has been acquired, bind it to use this network by default
                connectivityManager.bindProcessToNetwork(network);

                runOnUiThread(() -> {
                    onConnectClick(view, network);
                });
            }
        };
        connectivityManager.requestNetwork(
                new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                callback
        );
    }

    private void onConnectClick(View view, Network network) {
        setConnectedStatus(true, false);

        Thread thread = new Thread(this::runConnectionProcedure);
        thread.start();
    }
}