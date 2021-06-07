package com.cyclelution.RCTZebraBTPrinter;

import java.lang.reflect.Method;
import java.util.Set;
import javax.annotation.Nullable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.app.Activity;

import android.util.Log;
import android.util.Base64;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Callback;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;

import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.graphics.ZebraImageI;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import static com.cyclelution.RCTZebraBTPrinter.RCTZebraBTPrinterPackage.TAG;

@SuppressWarnings("unused")
public class RCTZebraBTPrinterModule extends ReactContextBaseJavaModule {

    // Debugging
    private static final boolean D = true;

    private final ReactApplicationContext reactContext;

    private Connection connection;
    private ZebraPrinter printer;

    private BluetoothAdapter bluetoothAdapter;

    private String delimiter = "";

    public RCTZebraBTPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private WritableMap deviceToWritableMap(BluetoothDevice device) {
        WritableMap params = Arguments.createMap();
    
        params.putString("name", device.getName());
        params.putString("address", device.getAddress());
        params.putString("id", device.getAddress());
    
        if (device.getBluetoothClass() != null) {
          params.putInt("class", device.getBluetoothClass().getDeviceClass());
        }
    
        return params;
      }

    @ReactMethod
    public void getBondedDevices(Promise promise) {
        WritableArray deviceList = Arguments.createArray();
            if (bluetoothAdapter != null) {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

            for (BluetoothDevice rawDevice : bondedDevices) {
                WritableMap device = deviceToWritableMap(rawDevice);
                deviceList.pushMap(device);
            }
        }
        promise.resolve(deviceList);
    }

    @ReactMethod
    public boolean isConnected() {
        if (connection == null) {
            return false;
        }
        return this.connection.isConnected();
    }

    private void openConnection() throws Exception {
        if (!this.isConnected()) {
            try {
                this.connection.open();
                Log.d(TAG, "Connection successfully opened");
                printer = ZebraPrinterFactory.getInstance(this.connection);
                PrinterLanguage pl = printer.getPrinterControlLanguage();
            } catch (ConnectionException e) {
                Log.d(TAG, "Couldn't init the connection");
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @ReactMethod
    public void initConnection(String macAddress) throws Exception {

        if (connection == null) {
            Log.d(TAG, "Init connection");
            this.connection = new BluetoothConnection(macAddress);
            this.openConnection();
            return;
        }

        if (!this.isConnected()) {
            this.openConnection();
            Log.d(TAG, "Connection reopened");
        }

        Log.d(TAG, "Connection was already initiated");
    }

    @ReactMethod
    public void printImport(String userPrinterSerial, String qrcode, String trackGen, String userCode, String transport, String date, Promise promise) throws Exception {
        try {

            if (connection == null) {
                Log.d(TAG, "Init connection");
                this.connection = new BluetoothConnection(userPrinterSerial);
                this.openConnection();
                return;
            }
    
            if (!this.isConnected()) {
                this.openConnection();
                Log.d(TAG, "Connection reopened");
            }

            String cpclConfigLabel = "! 0 200 200 799 1\r\nPW 639\r\nTONE 0\r\nSPEED 5\r\nON-FEED IGNORE\r\nNO-PACE\r\nPOSTFEED 40\r\nJOURNAL\r\nL 0 590 640 590 3\r\nL 0 311 640 311 3\r\nL 202 592 202 764 3\r\nB QR 200 20 M 2 U 10\r\nMA," + qrcode + "\r\nENDQR\r\nSCALE-TEXT ARIAL.TTF 15 15 130 245 "+ qrcode + "\r\nB 128 1 30 120 95 363 " + trackGen + "\r\nSCALE-TEXT ARIAL.TTF 15 15 70 520 " + trackGen + "\r\nSCALE-TEXT ARIAL.TTF 12 12 220 610 " + date + "\r\nSCALE-TEXT ARIAL.TTF 20 20 220 660 S 0015/EK\r\nPCX 15 610 !<NINJA_CG.PCX\r\nCOUNTRY CP874\r\nENCODING UTF-8\r\nSCALE-TEXT ANGSA.TTF 11 11 220 730 กรุณาพิมพ์ใบปะหน้าพัสดุนี้และติดบนกล่องพัสดุ\r\nPRINT\r\n";

            byte[]  configLabel = cpclConfigLabel.getBytes();

            connection.write(configLabel);

            if (connection instanceof BluetoothConnection) {

                String friendlyName = ((BluetoothConnection) connection).getFriendlyName();

                if (D) Log.d(TAG, "printLabel printed with " + friendlyName);

            }

        } catch (ConnectionException e) {

            if (D) Log.d(TAG, "printLabel com failed to open 2nd stage");
            promise.resolve(false);

        } finally {

            //disconnect();
            if (D) Log.d(TAG, "printLabel done");
            promise.resolve(true);

        }
    }

    @Override
    public String getName() {
        return "RCTZebraBTPrinter";
    }

}
