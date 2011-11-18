package es.mrjon.bluedroidduino;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends Activity {
  // Return Intent extra
  public static String EXTRA_DEVICE_ADDRESS = "device_address";

  // Member fields
  private BluetoothAdapter bluetoothAdapter;
  private ArrayAdapter<String> pairedDevicesArrayAdapter;
  private ArrayAdapter<String> newDevicesArrayAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Toast.makeText(
      this, "DeviceListActivity.onCreate", Toast.LENGTH_SHORT).show();


    // Setup the window
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.device_list);

    // Set result CANCELED incase the user backs out
    setResult(Activity.RESULT_CANCELED);

    // Initialize the button to perform device discovery
    Button scanButton = (Button) findViewById(R.id.button_scan);
    scanButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          doDiscovery();
          v.setVisibility(View.GONE);
        }
      });

    // Initialize array adapters. One for already paired devices and
    // one for newly discovered devices
    pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
    newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

    // Find and set up the ListView for paired devices
    ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
    pairedListView.setAdapter(pairedDevicesArrayAdapter);
    pairedListView.setOnItemClickListener(deviceClickListener);

    // Find and set up the ListView for newly discovered devices
    ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
    newDevicesListView.setAdapter(newDevicesArrayAdapter);
    newDevicesListView.setOnItemClickListener(deviceClickListener);

    // Register for broadcasts when a device is discovered
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    this.registerReceiver(receiver, filter);

    // Register for broadcasts when discovery has finished
    filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    this.registerReceiver(receiver, filter);

    // Get the local Bluetooth adapter
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Get a set of currently paired devices
    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

    // If there are paired devices, add each one to the ArrayAdapter
    if (pairedDevices.size() > 0) {
      findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
      for (BluetoothDevice device : pairedDevices) {
        pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
      }
    } else {
      String noDevices = getResources().getText(R.string.none_paired).toString();
      pairedDevicesArrayAdapter.add(noDevices);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Make sure we're not doing discovery anymore
    if (bluetoothAdapter != null) {
      bluetoothAdapter.cancelDiscovery();
    }

    // Unregister broadcast listeners
    this.unregisterReceiver(receiver);
  }

  private void doDiscovery() {
    Toast.makeText(
      this, "doDiscovery", Toast.LENGTH_SHORT).show();

    // Indicate scanning in the title
    setProgressBarIndeterminateVisibility(true);
    setTitle(R.string.scanning);

    // Turn on sub-title for new devices
    findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

    // If we're already discovering, stop it
    if (bluetoothAdapter.isDiscovering()) {
      bluetoothAdapter.cancelDiscovery();
    }

    // Request discover from BluetoothAdapter
    bluetoothAdapter.startDiscovery();
  }

  private OnItemClickListener deviceClickListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        // Cancel discovery because it's costly and we're about to connect
        bluetoothAdapter.cancelDiscovery();

        // Get the device MAC address, which is the last 17 chars in the View
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);

        // Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
      }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();

          // When discovery finds a device
          if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(
              BluetoothDevice.EXTRA_DEVICE);
            // If it's already paired, skip it, because it's been listed already
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
              newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title
          } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            setProgressBarIndeterminateVisibility(false);
            setTitle(R.string.select_device);
            if (newDevicesArrayAdapter.getCount() == 0) {
              String noDevices = getResources().getText(R.string.none_found).toString();
              newDevicesArrayAdapter.add(noDevices);
            }
          }
        }
      };


}
