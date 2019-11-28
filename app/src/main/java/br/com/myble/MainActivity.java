package br.com.myble;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements BluetoothInteractor.Listener {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothInteractor interactor;

    private int TIMEOUT = 10 * 1000;
    private int REQUEST_ENABLE_BT = 100;
    private Button button;
    private TextView textViewLog;
    private StringBuilder log = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.textView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log = new StringBuilder();
                interactor.requestPermission();
            }
        });
        interactor = new BluetoothInteractor(this, this);
        interactor.requestPermission();
        
        textViewLog = findViewById(R.id.textViewLog);
    }

    @Override
    public void onDataReceived(byte[] data) {
        System.out.println(data);
    }

    @Override
    public void onScanningChange(boolean enabled) {
        button.setText(enabled ? "Scanning..." : "Start");
        button.setEnabled(!enabled);
    }

    @Override
    public void onError(int errorCode) {
        setTitle("Error: " + String.valueOf(errorCode));
    }

    @Override
    public void onResult(String deviceName, String deviceId) {
        log.append(deviceName + " - " + deviceId + "\n");
        textViewLog.setText(log.toString());
    }

    @Override
    public void onPermissionChanged(BluetoothInteractor.Permission permission) {
        switch (permission) {
            case ACCESS_LOCATION_DENIED:
                requestLocationPermission();
                break;
            case GRANTED:
                interactor.scan(true, TIMEOUT);
                break;
            case DENIED:
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
                break;
        }
    }

    private void requestLocationPermission() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("This app needs location access");
        builder.setMessage("Please grant location access so this app can detect peripherals.");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            interactor.requestPermission();
        }

        if (requestCode == REQUEST_ENABLE_BT) {
            interactor.scan(true, TIMEOUT);
        }
    }
}
