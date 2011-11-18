package es.mrjon.bluedroidduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class BlueDuino extends Activity {
  private BluetoothAdapter bluetoothAdapter = null;

  // I don't know what this is for
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
    } else {
      Toast.makeText(
        this, "Bluetooth is available!", Toast.LENGTH_SHORT).show();
    }
  }

  public void onStart() {
    super.onStart();

    if (!bluetoothAdapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
  }
}
