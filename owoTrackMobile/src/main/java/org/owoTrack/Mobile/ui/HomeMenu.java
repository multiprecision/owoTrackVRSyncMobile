package org.owoTrack.Mobile.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.owoTrack.Mobile.MainActivity;
import org.owoTrack.Mobile.R;
import org.owoTrack.TrackingService;

import java.util.Map;

public class HomeMenu extends Fragment {


    public HomeMenu() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button autoConnectButton = view.findViewById(R.id.autoconnectButton);
        TextView sensorWarning = view.findViewById(R.id.sensorWarningTextView);
        sensorWarning.setText(MainActivity.getSensorText());
        LinearLayout sensorInfo = view.findViewById(R.id.layout_sensor_info);
        for (Map.Entry<String, Boolean> e : MainActivity.sensorExist.entrySet()) {
            View sensorView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_sensor_info, null, false);
            sensorInfo.addView(sensorView);
            ((TextView) sensorView.findViewById(R.id.sensor_name)).setText(e.getKey());
            ((ImageView) sensorView.findViewById(R.id.image_view)).setImageResource(e.getValue() ? R.drawable.not_missing : R.drawable.error_missing);
            Integer minDelay = MainActivity.sensorMinDelay.get(e.getKey());
            if (minDelay != null)
                ((TextView) sensorView.findViewById(R.id.sensor_min_delay)).setText("Min delay: " + minDelay + " μs");
        }

        if (MainActivity.missingRequiredSensor()) {
            TextView sleepWarning = view.findViewById(R.id.sleepWarningText);
            sleepWarning.setText("");
            autoConnectButton.setVisibility(View.GONE);
        } else {
            autoConnectButton.setOnClickListener(p -> autoConnect());
        }
    }

    private void autoConnect() {
        if (TrackingService.isInstanceCreated()) return;

        Intent mainIntent = new Intent(getContext(), TrackingService.class);
        mainIntent.putExtra("ipAddrTxt", "255.255.255.255");
        mainIntent.putExtra("port_no", 6969);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(mainIntent);
        } else {
            getContext().startService(mainIntent);
        }
        MainActivity.contr.navigate(R.id.connectFragment);
    }
}