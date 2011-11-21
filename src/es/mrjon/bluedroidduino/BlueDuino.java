package es.mrjon.bluedroidduino;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BlueDuino extends Activity {
  private BluetoothAdapter bluetoothAdapter = null;

  private BluetoothConnection connection = null;
  private BluetoothConnection.Future connectionFuture = null;

  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;

  private void debug(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();

  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (bluetoothAdapter == null) {
      debug("Bluetooth is not available");
      finish();
      return;
    }

    final EditText transmitTextBox = (EditText) findViewById(R.id.transmit_text);
    Button transmitButton = (Button) findViewById(R.id.button_transmit);
    transmitButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          synchronized (transmitTextBox) {
            Editable transmitText = transmitTextBox.getText();
            String text = transmitText.toString();
            transmitText.clear();
            try {
              // Disable and block until this is ready
              connection.write(text.getBytes());
              debug("Wrote message: " + text);
            } catch (IOException e) {
              debug("Write failed.");
            }
          }
        }
      });
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
      onSelectDeviceActivityResult(resultCode, data);
      break;
    case REQUEST_ENABLE_BT:
      onEnableBluetoothActivityResult(resultCode, data);
      break;
    }
  }

  private void onEnableBluetoothActivityResult(int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      // do something interesting?
    } else {
      debug("Setting up bluetooth failed.");
      finish();
    }
  }

  private void onSelectDeviceActivityResult(int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      String address = data.getExtras()
        .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
      BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

      debug("Connecting to: " + address);

      connectionFuture = new BluetoothConnection.Future(device);
      if (connectionFuture.failed()) {
        debug("Connection failed");
      } else {
        connection = connectionFuture.get();
        debug("Established connection to: " + address);
        // try {
        //   connection.write("+RR-".getBytes());
        //   debug("Writing message");
        // } catch (IOException e) {
        //   debug("Write failed.");
        // }
      }
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
