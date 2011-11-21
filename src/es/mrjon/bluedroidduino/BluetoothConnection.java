package es.mrjon.bluedroidduino;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnection {

  // Sets up a separate thread (ConnectThread) and runs it immediately,
  // connecting to the specified BluetoothDevice.  Execution can continue
  // in the meantime, but you can call get() or block() at any point to
  // wait for the connection to complete (it might complete unsucessfully).
  //
  // TODO(mrjones): This class is roughly what I want to do here, but it's
  // a little awkward to use, and the API could use some more thought.
  public static class ConnectionFuture {
//    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private final String address;

    private BluetoothConnection result;
    private boolean failed;
    private boolean done;
    private ConnectThread thread;
    
//    public ConnectionFuture(BluetoothDevice device) {
    public ConnectionFuture(String address) {
//      this.device = device;
      this.address = address;
      this.adapter = BluetoothAdapter.getDefaultAdapter();

      this.failed = false;
      this.done = false;

      this.thread = new ConnectThread(address, adapter, this);
      this.thread.start();
    }

    public BluetoothConnection get() {
      block();
      return result;
    }

    public void block() {
      // block
      while (done == false) {
        try {
          synchronized (thread) {
            thread.wait();
          }
        } catch (InterruptedException e) {
          // check the condition again, and maybe keep waiting
        }
      }
    }

    public boolean done() {
      return done;
    }

    public boolean failed() {
      return failed;
    }

    public void result(boolean success, BluetoothConnection connection) {
      this.failed = !success;
      this.result = connection;
      this.done = true;
    }
  }

  // Thread for doing blocking operations necessary to connect to a Bluetooth
  // device.  The result (success or failure) will be reported to a
  // BluetoothFuture class by calling the "result" method.
  private static class ConnectThread extends Thread {
    // See: http://developer.android.com/reference/android/bluetooth/
    //  BluetoothDevice.html#createRfcommSocketToServiceRecord(java.util.UUID)
    // "Hint: If you are connecting to a Bluetooth serial board then try using
    //  the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if
    //  you are connecting to an Android peer then please generate your own
    //  unique UUID."
    private static final UUID ARDUINO_UUID =
      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

//    private final BluetoothDevice device;
    private final String address;
    private final BluetoothAdapter adapter;
    private final ConnectionFuture connectionFuture;

    public ConnectThread(
//      BluetoothDevice device,
      String address,
      BluetoothAdapter adapter,
      ConnectionFuture connectionFuture) {
//      this.device = device;
      this.address = address;
      this.adapter = adapter;
      this.connectionFuture = connectionFuture;
    }

    public void run() {
      adapter.cancelDiscovery();

      BluetoothDevice device = adapter.getRemoteDevice(address);
      BluetoothSocket socket;

      try {
        Log.i("BluetoothConnection", "Connecting...");
        socket = device.createRfcommSocketToServiceRecord(
          ARDUINO_UUID);
        socket.connect();
        connectionFuture.result(true, new BluetoothConnection(device, socket));
        Log.i("BluetoothConnection", "Done!");
      } catch (IOException e) {
        Log.d("BluetoothConnection", Log.getStackTraceString(e));
        connectionFuture.result(false, null);
      }

      synchronized(this) {
        notifyAll();
      }
    }
  }

  private final BluetoothAdapter adapter;
  private final BluetoothDevice device;
  private final BluetoothSocket socket;

  private final OutputStream outStream;

  public BluetoothConnection(BluetoothDevice device, BluetoothSocket socket)
      throws IOException {
    this.device = device;
    this.socket = socket;

    this.outStream = socket.getOutputStream();

    this.adapter = BluetoothAdapter.getDefaultAdapter();
  }

  public void write(byte[] data) throws IOException {
    outStream.write(data);
  }
}
