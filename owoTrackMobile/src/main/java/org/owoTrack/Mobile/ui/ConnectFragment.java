package org.owoTrack.Mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputLayout;

import org.owoTrack.Mobile.MainActivity;
import org.owoTrack.Mobile.R;
import org.owoTrack.TrackingService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectFragment extends GenericBindingFragment {

    Button connect_button = null;
    EditText ipAddrTxt = null;
    EditText portTxt = null;
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

    public static SharedPreferences getSharedPreferences(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static ConnectFragment newInstance() {
        ConnectFragment fragment = new ConnectFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SharedPreferences getSharedPreferences() {
        return getSharedPreferences(getContext());
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

    }

    @Override
    public void onDestroy() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
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

        EditText madgwickInput = curr_view.findViewById(R.id.input_madgwick_beta);
        TextInputLayout madgwickInputLayout = curr_view.findViewById(R.id.input_layout_madgwick_beta);
        TextView madgwickText = curr_view.findViewById(R.id.text_madgwick_beta);

        SharedPreferences prefs = getSharedPreferences();
        AutoCompleteTextView sensorTextView = curr_view.findViewById(R.id.input_sensor_type);
        String[] sensorArray = requireContext().getResources().getStringArray(R.array.sensor_type_dropdown);
        ArrayAdapter<String> sensorAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sensorArray);
        sensorTextView.setAdapter(sensorAdapter);
        sensorTextView.setOnItemClickListener((parent, view, position, id) -> {
            prefs.edit().putInt("sensor_type", position).apply();
            madgwickInput.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
            madgwickInputLayout.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
            madgwickText.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
        });
        int savedSensorType = prefs.getInt("sensor_type", 0);
        sensorTextView.setText(sensorArray[savedSensorType], false);
        sensorTextView.setThreshold(Integer.MAX_VALUE);

        ipAddrTxt.setText(prefs.getString("ip_address", ""));
        portTxt.setText(String.valueOf(prefs.getInt("port", 6969)));


        madgwickInput.setText(String.valueOf(prefs.getFloat("madgwick_beta", 0.033f)));
        madgwickInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float beta = Float.parseFloat(s.toString());
                    prefs.edit().putFloat("madgwick_beta", beta).apply();
                } catch (Exception ignored) {
                }
            }
        });

        connect_button.setOnClickListener(v -> onConnect(false));

        prefs.registerOnSharedPreferenceChangeListener(listener);

        onConnectionStatus(TrackingService.isInstanceCreated());

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

    private void onConnect(boolean auto) {
        if ((service_v != null) && (service_v.is_running())) {
            onSetStatus("Killing service...");
            Intent intent = new Intent("kill_service");
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(mainIntent);
        } else {
            getContext().startService(mainIntent);
        }
    }

    public void save_data() {
        if (MainActivity.missingRequiredSensor()) return;

        if (ipAddrTxt == null || portTxt == null) return;

        SharedPreferences prefs = getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("ip_address", get_ip_address());
        editor.putInt("port", get_port());

        editor.apply();
    }
}