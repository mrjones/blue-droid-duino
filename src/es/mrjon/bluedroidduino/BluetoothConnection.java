package es.mrjon.bluedroidduino;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
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
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private final Handler readHandler;

    private BluetoothConnection result;
    private boolean failed;
    private boolean done;
    private ConnectThread thread;
    
    /**
     * @param device The BluetoothDevice to establish a connection to
     * @param readHandler A handler to notify when data is recieved from
     *  the connection.
     */
    public ConnectionFuture(BluetoothDevice device, Handler readHandler) {
      this.device = device;
      this.readHandler = readHandler;
      this.adapter = BluetoothAdapter.getDefaultAdapter();

      this.failed = false;
      this.done = false;

      this.thread = new ConnectThread(device, adapter, this);
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

    public void socketEstablished(BluetoothSocket socket) throws IOException {
      this.failed = false;
      this.result = new BluetoothConnection(device, socket, readHandler);
      this.done = true;
    }

    public void socketFailed() {
      this.failed = true;
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

    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private final ConnectionFuture connectionFuture;

    public ConnectThread(
      BluetoothDevice device,
      BluetoothAdapter adapter,
      ConnectionFuture connectionFuture) {
      this.device = device;
      this.adapter = adapter;
      this.connectionFuture = connectionFuture;
    }

    public void run() {
      adapter.cancelDiscovery();

      BluetoothSocket socket;

      try {
        Log.i("BluetoothConnection", "Connecting...");
        socket = device.createRfcommSocketToServiceRecord(
          ARDUINO_UUID);
        socket.connect();
        connectionFuture.socketEstablished(socket);
        Log.i("BluetoothConnection", "Done!");
      } catch (IOException e) {
        Log.d("BluetoothConnection", Log.getStackTraceString(e));
        connectionFuture.socketFailed();
      }

      synchronized(this) {
        notifyAll();
      }
    }
  }

  private static class ReaderThread extends Thread {
    private final InputStream inputStream;
    private final Handler readHandler;

    public ReaderThread(InputStream inputStream, Handler readHandler) {
      this.inputStream = inputStream;
      this.readHandler = readHandler;
    }

    public void run() {
      setName("ReaderThread");

      byte[] buffer = new byte[1024];
      int numBytes;

      while(true) {
        try {
          numBytes = inputStream.read(buffer);
          if (readHandler != null) {
            readHandler.obtainMessage(MESSAGE_READ, numBytes, -1, buffer)
              .sendToTarget();
          }
        } catch (IOException e) {
          // handle
        }
      }
    }
  }

  private final BluetoothDevice device;
  private final BluetoothSocket socket;
  private final ReaderThread readerThread;

  private final OutputStream outStream;

    public static final int MESSAGE_READ = 1;

  public BluetoothConnection(
    BluetoothDevice device, BluetoothSocket socket, Handler readHandler)
      throws IOException {
    this.device = device;
    this.socket = socket;

    this.readerThread = new ReaderThread(socket.getInputStream(), readHandler);
    this.outStream = socket.getOutputStream();

    this.readerThread.start();
  }

  public void write(byte[] data) throws IOException {
    outStream.write(data);
  }
}
