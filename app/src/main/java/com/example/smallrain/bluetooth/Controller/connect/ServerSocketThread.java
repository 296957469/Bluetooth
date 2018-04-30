package com.example.smallrain.bluetooth.Controller.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;


/**服务端
 * Created by SmallRain on 2018/4/29.
 */

public class ServerSocketThread extends Thread {
    private static final String NAME = "BlueToothClass";
    private static final UUID MY_UUID = UUID.fromString(Constant.CONNECTION_UUID);
    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private ConnectedThread connectedThread;

    public ServerSocketThread(BluetoothAdapter adapter, Handler handler) {
        this.bluetoothAdapter = adapter;
        this.handler = handler;
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (Exception e) {

        }
    }
    @Override
    public void run() {
        BluetoothSocket socket = null;
        while (true) {
            try {
                handler.sendEmptyMessage(Constant.MSG_START_LISTENING);
                socket = serverSocket.accept();
            } catch (IOException e) {
                handler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING);
                break;
            }
                manageConnectedSocket(socket);
        }
    }
    private  void manageConnectedSocket(BluetoothSocket socket) {
        Message message=handler.obtainMessage(Constant.MSG_GOT_A_CLIENT,socket.getRemoteDevice().getName());
        handler.sendMessage(message);
        connectedThread = new ConnectedThread(socket, handler);
        connectedThread.start();
    }

    /**
     * 服务器发送消息给客户端  基本不用这个方法
     * @param data
     */
   public  void sendData(byte[]data){
       if(connectedThread!=null){
           connectedThread.write(data);
       }
   }

    /**
     * 关闭服务器
     */
    public  void cancel(){
        try{
            serverSocket.close();
            handler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING);
        }catch (IOException e){
            Message message=handler.obtainMessage(Constant.MSG_ERROR,"服务器关闭失败");
            handler.sendMessage(message);
        }
    }
}




















