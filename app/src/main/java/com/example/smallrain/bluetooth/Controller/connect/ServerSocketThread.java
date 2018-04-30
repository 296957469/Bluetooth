package com.example.smallrain.bluetooth.Controller.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

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
                handler.sendMessage(handler.obtainMessage(Constant.MSG_ERROR, e));
                break;
            }
            //目前测试使用，如果有一个连接连接上来，则将服务器关闭
            if(socket!=null){
                manageConnectedSocket(socket);
                try{
                    //关闭服务器 如果想一直开启服务器接受多个连接，则注释下面的代码
                    serverSocket.close();
                    handler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING);
                }catch (IOException e){

                }
                break;
            }
        }
    }
    private  void manageConnectedSocket(BluetoothSocket socket) {
        //关闭服务器 如果想一直开启服务器接受多个连接，则注释下面的if代码
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        handler.sendEmptyMessage(Constant.MSG_GOT_A_CLIENT);
        connectedThread = new ConnectedThread(socket, handler);
        connectedThread.start();
    }

    /**
     * 服务器发送消息给客户端
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

        }
    }
}




















