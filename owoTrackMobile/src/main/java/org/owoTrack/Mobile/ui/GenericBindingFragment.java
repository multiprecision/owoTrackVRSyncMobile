package org.owoTrack.Mobile.ui;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.owoTrack.Mobile.MainActivity;
import org.owoTrack.TrackingService;

public abstract class GenericBindingFragment extends Fragment {

    public static String status_string = "";
    View curr_view;
    TrackingService service_v = null;
    private ServiceConnection trackingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.TrackingBinder binderBridge = (TrackingService.TrackingBinder) service;
            service_v = binderBridge.getService();

            onConnectionStatus(service_v.is_running());

            if ((!service_v.is_running()) && (status_string.length() > 0)) {
                onSetStatus(status_string);
            } else {
                status_string = service_v.getLog();
                onSetStatus(status_string);
            }
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
                    status_string = data + "\n" + status_string;
                    return;
                case "disconnect":
                    onConnectionStatus(false);
                    return;
                case "reconnect-service":
                    doBinding(false);
                    doBinding(true);
                    // Navigation.findNavController(curr_view).navigateUp();

            }
        }
    };

    protected abstract void onSetStatus(String to);

    protected abstract void onConnectionStatus(boolean to);

    private void doBinding(boolean is_bound) {
        if (is_bound) {
            Intent intent = new Intent(getContext(), TrackingService.class);
            getContext().bindService(intent, trackingConnection, BIND_AUTO_CREATE);
        } else {
            getContext().unbindService(trackingConnection);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!MainActivity.hasAnySensorsAtAll()) return;
        doBinding(false);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(logReceiver);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MainActivity.hasAnySensorsAtAll()) return;

        doBinding(true);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, new IntentFilter("info-log"));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, new IntentFilter("disconnect"));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, new IntentFilter("reconnect-service"));
    }
}
