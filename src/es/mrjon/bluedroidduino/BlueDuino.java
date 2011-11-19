package es.mrjon.bluedroidduino;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class BlueDuino extends Activity {
  private BluetoothAdapter bluetoothAdapter = null;
  private BluetoothConnection connection = null;
  private BluetoothConnection.Future connectionFuture = null;

  // I don't know what this is for
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;



  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (bluetoothAdapter == null) {
      Toast.makeText(
        this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
  }

  public void onStart() {
    super.onStart();

    if (!bluetoothAdapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    Intent serverIntent = new Intent(this, DeviceListActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case REQUEST_CONNECT_DEVICE:
      // When DeviceListActivity returns with a device to connect
      if (resultCode == Activity.RESULT_OK) {
        // Get the device MAC address
        String address = data.getExtras()
          .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        Toast.makeText(
          this, "Connecting to: " + address, Toast.LENGTH_SHORT).show();

        connectionFuture = new BluetoothConnection.Future(device);
        connection = connectionFuture.get();

        Toast.makeText(
          this, "Established connection to: " + address, Toast.LENGTH_LONG).show();
        try {
            connection.write("HI!".getBytes());
        } catch (IOException e) {
        Toast.makeText(
          this, "Write failed.", Toast.LENGTH_LONG).show();

        }
      }
      break;
    case REQUEST_ENABLE_BT:
      if (resultCode == Activity.RESULT_OK) {
        Toast.makeText(
          this, "Bluetooth activated.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(
          this, "Setting up bluetooth failed.", Toast.LENGTH_SHORT).show();
        finish();
      }
      break;
    }
  }


  // @Override
  // public boolean onOptionsItemSelected(MenuItem item) {
  //       switch (item.getItemId()) {
  //       case R.id.scan:
  //           // Launch the DeviceListActivity to see devices and do scan
  //           Intent serverIntent = new Intent(this, DeviceListActivity.class);
  //           startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
  //           return true;
  //       case R.id.discoverable:
  //           // Ensure this device is discoverable by others
  //           ensureDiscoverable();
  //           return true;
  //       }
  //       return false;
  //   }
}
