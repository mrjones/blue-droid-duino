package es.mrjon.bluedroidduino;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnection {

  public static class Future {
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;

    private BluetoothConnection result;
    private boolean failed;
    private boolean done;
    private ConnectThread thread;
    
    public Future(BluetoothDevice device) {
      this.device = device;
      this.adapter = BluetoothAdapter.getDefaultAdapter();

      this.failed = false;
      this.done = false;

      this.thread = new ConnectThread(device, adapter, this);
      this.thread.run();
    }

    public BluetoothConnection get() {
      // block
      while (done == false) {
        try {
          thread.wait();
        } catch (InterruptedException e) {
          // check the condition again, and maybe keep waiting
        }
      }
      return result;
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

  private static class ConnectThread extends Thread {
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private final Future connectionFuture;

    public ConnectThread(
      BluetoothDevice device,
      BluetoothAdapter adapter,
      Future connectionFuture) {
      this.device = device;
      this.adapter = adapter;
      this.connectionFuture = connectionFuture;
    }

    public void run() {
      adapter.cancelDiscovery();

      BluetoothSocket socket;

      try {
        socket = this.device.createRfcommSocketToServiceRecord(
          UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//          UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));

        socket.connect();
        connectionFuture.result(true, new BluetoothConnection(device, socket));
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
