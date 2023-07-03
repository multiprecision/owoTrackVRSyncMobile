package org.owoTrack.Mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.owoTrack.Mobile.MainActivity;
import org.owoTrack.Mobile.R;
import org.owoTrack.TrackingService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectFragment extends GenericBindingFragment {

    final static String CONN_DATA = "CONNECTION_DATA_PREF";
    Button connect_button = null;
    EditText ipAddrTxt = null;
    EditText portTxt = null;
    CheckBox magBox = null;
    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, java.lang.String s) {
            if (s.equals("ip_address")) {
                ipAddrTxt.setText(sharedPreferences.getString(s, ""));
            }
            if (s.equals("port")) {
                portTxt.setText(String.valueOf(sharedPreferences.getInt(s, 6969)));
            }
        }
    };

    public ConnectFragment() {
    }

    public static SharedPreferences get_prefs(Context c) {
        return c.getSharedPreferences(CONN_DATA, Context.MODE_PRIVATE);
    }

    public static ConnectFragment newInstance() {
        ConnectFragment fragment = new ConnectFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SharedPreferences get_prefs() {
        return get_prefs(getContext());
    }

    @Override
    protected void onSetStatus(String to) {
        if (curr_view == null) return;

        TextView text = curr_view.findViewById(R.id.statusText);

        if (text != null)
            text.setText(to.split("\n")[0]);
    }

    @Override
    protected void onConnectionStatus(boolean to) {
        if (connect_button != null)
            connect_button.setText(to ? "Disconnect" : "Connect");

        if (magBox != null)
            magBox.setEnabled(!to);
    }

    @Override
    public void onDestroy() {
        get_prefs().unregisterOnSharedPreferenceChangeListener(listener);
        save_data();

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        curr_view = inflater.inflate(R.layout.fragment_connect, container, false);

        connect_button = curr_view.findViewById(R.id.connectButton);
        ipAddrTxt = curr_view.findViewById(R.id.editIP);
        portTxt = curr_view.findViewById(R.id.editPort);
        magBox = curr_view.findViewById(R.id.magnetCheckbox);


        if (!MainActivity.hasAnySensorsAtAll()) {
            connect_button.setEnabled(false);
            ipAddrTxt.setEnabled(false);
            portTxt.setEnabled(false);
            magBox.setEnabled(false);

            TextView statusText = curr_view.findViewById(R.id.statusText);
            statusText.setText(R.string.sensors_missing_all);
        } else {
            SharedPreferences prefs = get_prefs();

            ipAddrTxt.setText(prefs.getString("ip_address", ""));
            portTxt.setText(String.valueOf(prefs.getInt("port", 6969)));
            magBox.setChecked(prefs.getBoolean("magnetometer", true));

            connect_button.setOnClickListener(v -> onConnect(false));

            prefs.registerOnSharedPreferenceChangeListener(listener);

            onConnectionStatus(TrackingService.isInstanceCreated());
        }

        return curr_view;
    }

    private String get_ip_address() {
        String filtered_ip = String.valueOf(ipAddrTxt.getText()).replaceAll("[^0-9\\.]", "");
        ipAddrTxt.setText(filtered_ip);

        return filtered_ip;
    }

    private int get_port() {
        String filtered_port = String.valueOf(portTxt.getText()).replaceAll("[^0-9]", "");
        portTxt.setText(filtered_port);

        int val = 6969;
        try {
            val = Integer.parseInt(filtered_port);
        } catch (NumberFormatException ignored) {
        }

        return val;
    }

    private boolean get_mag() {
        return magBox.isChecked();
    }

    private void onConnect(boolean auto) {
        if ((service_v != null) && (service_v.is_running())) {
            onSetStatus("Killing service...");
            Intent intent = new Intent("kill-ze-service");
            getContext().sendBroadcast(intent);
            return;
        }


        onConnectionStatus(true);

        Intent mainIntent = new Intent(getContext(), TrackingService.class);
        if (auto) {
            mainIntent.putExtra("ipAddrTxt", "255.255.255.255");
        } else {
            mainIntent.putExtra("ipAddrTxt", get_ip_address());
        }

        mainIntent.putExtra("port_no", get_port());
        mainIntent.putExtra("magnetometer", get_mag());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(mainIntent);
        } else {
            getContext().startService(mainIntent);
        }
    }

    public void save_data() {
        if (!MainActivity.hasAnySensorsAtAll()) return;

        if (ipAddrTxt == null || portTxt == null || magBox == null) return;

        SharedPreferences prefs = get_prefs();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("ip_address", get_ip_address());
        editor.putInt("port", get_port());
        editor.putBoolean("magnetometer", get_mag());

        editor.apply();
    }
}