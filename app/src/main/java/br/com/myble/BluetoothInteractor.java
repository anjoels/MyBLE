package br.com.myble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;
import androidx.core.app.Person;

import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

@RequiresApi(api = Build.VERSION_CODES.M)
public class BluetoothInteractor {

    enum Permission {
        ACCESS_LOCATION_DENIED,
        GRANTED,
        DENIED
    }

    interface Listener {
        void onDataReceived(byte[] data);
        void onScanningChange(boolean enabled);
        void onError(int errorCode);
        void onResult(String deviceName, String deviceId);
        void onPermissionChanged(Permission permission);
    }

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothManager bluetoothManager;
    private final BluetoothLeScanner bluetoothScanner;
    private final Context context;
    private final Listener listener;

    private boolean isScanning = false;
    private Handler handler = new Handler();
    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            publishScanResult(result);
        }
    };

    public BluetoothInteractor(Listener listener, Context context) {
        this.listener = listener;
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
    }


    public void requestPermission() {

        if (context.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onPermissionChanged(Permission.ACCESS_LOCATION_DENIED);
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            listener.onPermissionChanged(Permission.DENIED);
        } else {
            listener.onPermissionChanged(Permission.GRANTED);
        }
    }

    public void scan(boolean enabled, int timeout) {
        if(enabled) {
            isScanning = true;
            listener.onScanningChange(isScanning);
            bluetoothScanner.startScan(buildFilters(), buildSettings(), callback);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    listener.onScanningChange(isScanning);
                    bluetoothScanner.stopScan(callback);
                }
            }, timeout);
        } else {
            isScanning = false;
            listener.onScanningChange(isScanning);
            bluetoothScanner.stopScan(callback);
        }
    }

    private List<ScanFilter> buildFilters() {
        return Arrays.asList(new ScanFilter.Builder().build());
    }

    private ScanSettings buildSettings() {
        return new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    }

    private void publishScanResult(ScanResult scanResult) {
        BluetoothDevice device = scanResult.getDevice();
        listener.onResult(device.getName(), device.getAddress());
    }
}
