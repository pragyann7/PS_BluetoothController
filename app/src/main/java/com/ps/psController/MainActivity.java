package com.ps.psController;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private final ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private BluetoothDevice connectedDevice;
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String lastCommand = "";
    private boolean isConnected = false;
    private boolean useArrows = false;
    private boolean useAuto = false;

    private enum LayoutState { MAIN, SCAN, GAMING, DIMMER, SWITCHES }
    private LayoutState currentLayout = LayoutState.MAIN;

    private static class SwitchData {
        String name, onCmd, offCmd;
        SwitchData(String name, String onCmd, String offCmd) {
            this.name = name; this.onCmd = onCmd; this.offCmd = offCmd;
        }
    }
    private final ArrayList<SwitchData> customSwitches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceAdapter = new DeviceAdapter();
        initMainLayout();
    }

    private void initMainLayout() {
        currentLayout = LayoutState.MAIN;
        setContentView(R.layout.activity_main);
        updateStatusIndicator();

        Button btnJoystick = findViewById(R.id.joystick);
        Button btnDimmer = findViewById(R.id.button2);
        Button btnSwitches = findViewById(R.id.button3);
        FloatingActionButton btnBluetooth = findViewById(R.id.bluetoothBtn);

        if (btnJoystick != null) btnJoystick.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initGamingPadLayout();
        });
        if (btnDimmer != null) btnDimmer.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initDimmerLayout();
        });
        if (btnSwitches != null) btnSwitches.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initSwitchesLayout();
        });
        if (btnBluetooth != null) btnBluetooth.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initScanLayout();
        });
    }

    private void initScanLayout() {
        currentLayout = LayoutState.SCAN;
        setContentView(R.layout.scan_device);
        updateStatusIndicator();

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnScan = findViewById(R.id.btnScan);
        ListView deviceListView = findViewById(R.id.deviceList);

        if (deviceListView != null) {
            deviceListView.setAdapter(deviceAdapter);
            deviceListView.setOnItemClickListener((parent, view, position, id) -> {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                connectToDevice(devicesList.get(position));
            });
        }

        if (btnScan != null) btnScan.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showPairedDevices();
        });
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initMainLayout();
        });
    }

    private long lastSendTime = 0;

    @SuppressLint("ClickableViewAccessibility")
    private void initGamingPadLayout() {
        currentLayout = LayoutState.GAMING;
        setContentView(R.layout.gmaing_pad);
        updateStatusIndicator();

        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                initMainLayout();
            });
        }

        JoystickView joystick = findViewById(R.id.joystickView);
        TextView outputDisplay = findViewById(R.id.outputDisplay);
        View arrowKeysContainer = findViewById(R.id.arrowKeysContainer);
        SwitchMaterial arrowSwitch = findViewById(R.id.switch1);

        if (arrowSwitch != null) {
            arrowSwitch.setChecked(!useArrows);

            // Initial visibility update
            updateControlsVisibility(useArrows, joystick, arrowKeysContainer);

            arrowSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                useArrows = !isChecked;
                updateControlsVisibility(!isChecked, joystick, arrowKeysContainer);
            });
        }

        setupArrowButton(R.id.btnUp, "0,-100\n", "0,0\n", "FORWARD", outputDisplay);
        setupArrowButton(R.id.btnDown, "0,100\n", "0,0\n", "BACKWARD", outputDisplay);
        setupArrowButton(R.id.btnLeft, "-100,0\n", "0,0\n", "LEFT", outputDisplay);
        setupArrowButton(R.id.btnRight, "100,0\n", "0,0\n", "RIGHT", outputDisplay);

        SwitchMaterial autoSwitch = findViewById(R.id.switchAuto);
        if (autoSwitch != null) {
            autoSwitch.setChecked(useAuto);
            autoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                useAuto = isChecked;
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                updateControlsVisibility(useArrows, joystick, arrowKeysContainer);
                if (isChecked) {
                    sendData("A_MODE\n");
                    if (outputDisplay != null) {
                        outputDisplay.setText("AUTO MODE");
                    }
                } else {
                    sendData("M_MODE\n");
                    if (outputDisplay != null) {
                        outputDisplay.setText("MANUAL MODE");
                    }
                }
            });
        }

        if (joystick != null) {
            joystick.setJoystickListener((xPercent, yPercent, id) -> {
                if (useAuto) return; // Block in Auto mode
                if (useArrows) return; // Ignore joystick if arrow mode is active

                int x = (int)(xPercent * 100);
                int y = (int)(yPercent * 100);

                // Dead zone
                if (Math.abs(x) < 10) x = 0;
                if (Math.abs(y) < 10) y = 0;

                // AUTO STOP when released
                if (x == 0 && y == 0) {

                    sendData("0,0\n");

                    if (outputDisplay != null) {
                        outputDisplay.setText("STOP");
                    }

                    return;
                }

                // Direction display
                String direction = "STOP";

                StringBuilder sb = new StringBuilder();

                if (y <= -20) sb.append("FORWARD");
                else if (y >= 20) sb.append("BACKWARD");

                if (x >= 20) {

                    if (sb.length() > 0) sb.append(" + ");

                    sb.append("RIGHT");

                } else if (x <= -20) {

                    if (sb.length() > 0) sb.append(" + ");

                    sb.append("LEFT");
                }

                if (sb.length() > 0) {
                    direction = sb.toString();
                }

                if (outputDisplay != null) {
                    outputDisplay.setText(direction);
                }

                // Send joystick data
                String command = x + "," + y + "\n";

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastSendTime > 30) {

                    sendData(command);

                    lastSendTime = currentTime;
                }
            });
        }

        setupGamingButton(R.id.btnA, "A_ON\n", "A_OFF\n", "BUTTON A", outputDisplay);
        setupGamingButton(R.id.btnB, "B_ON\n", "B_OFF\n", "BUTTON B", outputDisplay);
        setupGamingButton(R.id.btnX, "X_ON\n", "X_OFF\n", "BUTTON X", outputDisplay);
        setupGamingButton(R.id.btnY, "Y_ON\n", "Y_OFF\n", "BUTTON Y", outputDisplay);
    }

    private void updateControlsVisibility(boolean isArrowMode, View joystick, View arrowKeysContainer) {
        if (isArrowMode) {
            if (joystick != null) joystick.setVisibility(View.GONE);
            if (arrowKeysContainer != null) arrowKeysContainer.setVisibility(View.VISIBLE);

            // Hide action buttons in landscape if present
            View actionButtons = findViewById(R.id.constraintLayout3);
            if (actionButtons != null) actionButtons.setVisibility(View.GONE);

            // Hide action buttons in portrait if present
            int[] portraitActionButtons = {R.id.btnA, R.id.btnB, R.id.btnX, R.id.btnY};
            for (int id : portraitActionButtons) {
                View v = findViewById(id);
                if (v != null) v.setVisibility(View.GONE);
            }
        } else {
            if (joystick != null) joystick.setVisibility(View.VISIBLE);
            if (arrowKeysContainer != null) arrowKeysContainer.setVisibility(View.GONE);

            View actionButtons = findViewById(R.id.constraintLayout3);
            if (actionButtons != null) actionButtons.setVisibility(View.VISIBLE);

            int[] portraitActionButtons = {R.id.btnA, R.id.btnB, R.id.btnX, R.id.btnY};
            for (int id : portraitActionButtons) {
                View v = findViewById(id);
                if (v != null) v.setVisibility(View.VISIBLE);
            }
        }

        // Apply Auto mode visual block
        float alpha = useAuto ? 0.3f : 1.0f;
        if (joystick != null) {
            joystick.setAlpha(alpha);
        }
        if (arrowKeysContainer != null) {
            arrowKeysContainer.setAlpha(alpha);
        }
        View actionButtons = findViewById(R.id.constraintLayout3);
        if (actionButtons != null) {
            actionButtons.setAlpha(alpha);
        }
        int[] actionIds = {R.id.btnA, R.id.btnB, R.id.btnX, R.id.btnY};
        for (int id : actionIds) {
            View v = findViewById(id);
            if (v != null) v.setAlpha(alpha);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGamingButton(int id, String pressCmd, String releaseCmd, String label, TextView display) {
        Button btn = findViewById(id);
        if (btn != null) {
            btn.setOnTouchListener((v, event) -> {
                if (useAuto) return false; // Blocked in Auto mode
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        sendData(pressCmd);
                        if (display != null) display.setText(label);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        sendData(releaseCmd);
                        if (display != null) display.setText("STOP");
                        return true;
                }
                return false;
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupArrowButton(int id, String pressCmd, String releaseCmd, String label, TextView display) {
        Button btn = findViewById(id);
        if (btn != null) {
            btn.setOnTouchListener((v, event) -> {
                if (useAuto) return false; // Blocked in Auto mode
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        sendData(pressCmd);
                        if (display != null) display.setText(label);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        sendData(releaseCmd);
                        if (display != null) display.setText("STOP");
                        return true;
                }
                return false;
            });
        }
    }

    private void initDimmerLayout() {
        currentLayout = LayoutState.DIMMER;
        setContentView(R.layout.dimmer_slider);
        updateStatusIndicator();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initMainLayout();
        });

        CircularDimmerView dimmerView = findViewById(R.id.dimmerView);
        if (dimmerView != null) {
            dimmerView.setDimmerListener(progress -> {
                // Send only the progress value
                sendData(progress + "\n");
            });
        }
    }

    private void initSwitchesLayout() {
        currentLayout = LayoutState.SWITCHES;
        setContentView(R.layout.switches_control);
        updateStatusIndicator();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            initMainLayout();
        });

        ViewGroup container = findViewById(R.id.switchesContainer);
        FloatingActionButton btnAdd = findViewById(R.id.btnAddSwitch);

        // Restore custom switches
        for (SwitchData data : customSwitches) {
            addSwitchToView(container, data);
        }

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showAddSwitchDialog(container);
            });
        }
    }

    private void showAddSwitchDialog(ViewGroup container) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Switch");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Switch Name (e.g. Fan)");
        layout.addView(inputName);

        final EditText inputOn = new EditText(this);
        inputOn.setHint("ON Command (e.g. F_ON)");
        layout.addView(inputOn);

        final EditText inputOff = new EditText(this);
        inputOff.setHint("OFF Command (e.g. F_OFF)");
        layout.addView(inputOff);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString();
            String onCmd = inputOn.getText().toString();
            String offCmd = inputOff.getText().toString();

            if (!name.isEmpty() && !onCmd.isEmpty() && !offCmd.isEmpty()) {
                SwitchData data = new SwitchData(name, onCmd, offCmd);
                customSwitches.add(data);
                addSwitchToView(container, data);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addSwitchToView(ViewGroup container, SwitchData data) {
        if (container == null) return;
        View switchView = getLayoutInflater().inflate(R.layout.item_switch, container, false);

        TextView tvName = switchView.findViewById(R.id.switchName);
        SwitchMaterial toggle = switchView.findViewById(R.id.switchToggle);
        ImageButton btnDelete = switchView.findViewById(R.id.btnDelete);

        if (tvName != null) tvName.setText(data.name);
        if (toggle != null) {
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendData(isChecked ? data.onCmd : data.offCmd);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                new AlertDialog.Builder(this)
                        .setTitle("Delete Switch")
                        .setMessage("Are you sure you want to delete '" + data.name + "'?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            customSwitches.remove(data);
                            container.removeView(switchView);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        container.addView(switchView);
    }



    private void showPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        devicesList.clear();
        devicesList.addAll(pairedDevices);
        deviceAdapter.notifyDataSetChanged();
    }

    private class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        DeviceAdapter() {
            super(MainActivity.this, android.R.layout.simple_list_item_1, devicesList);
        }

        @androidx.annotation.NonNull
        @Override
        public View getView(int position, View convertView, @androidx.annotation.NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text = view.findViewById(android.R.id.text1);
            BluetoothDevice device = devicesList.get(position);

            String name = "Unknown Device";
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    name = device.getName();
                }
            } catch (Exception ignored) {}

            text.setText(name + "\n" + device.getAddress());

            if (isConnected && connectedDevice != null && connectedDevice.getAddress().equals(device.getAddress())) {
                view.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")); // Light green highlight
                text.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // Dark green text
            } else {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                text.setTextColor(android.graphics.Color.parseColor("#212121")); // Hardcoded dark gray
            }
            return view;
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }

        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            outputStream = socket.getOutputStream();
            isConnected = true;
            connectedDevice = device;
            updateStatusIndicator();
            deviceAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            isConnected = false;
            connectedDevice = null;
            updateStatusIndicator();
            deviceAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusIndicator() {
        TextView tvStatus = findViewById(R.id.connectionStatus);
        if (tvStatus != null) {
            if (isConnected) {
                tvStatus.setText("Connected");
                tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
            } else {
                tvStatus.setText("Disconnected");
                tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336")); // Red
            }
        }
    }

    private void sendData(String data) {

        if (outputStream == null) {
            return;
        }

        try {

            outputStream.write(data.getBytes());

            // Optional but recommended
            outputStream.flush();

        } catch (Exception e) {

            e.printStackTrace();
            isConnected = false;
            updateStatusIndicator();

        }
    }

    @Override
    public void onConfigurationChanged(@androidx.annotation.NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Refresh the current layout to load the landscape version if available
        switch (currentLayout) {
            case MAIN: initMainLayout(); break;
            case SCAN: initScanLayout(); break;
            case GAMING: initGamingPadLayout(); break;
            case DIMMER: initDimmerLayout(); break;
            case SWITCHES: initSwitchesLayout(); break;
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.homeTitle) == null) {
            initMainLayout();
        } else {
            super.onBackPressed();
        }
    }
}