package com.example.smallrain.bluetooth.Controller.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**客户端
 * Created by SmallRain on 2018/4/29.
 */

public class ClientThread extends Thread {
    private static final UUID MY_UUID = UUID.fromString(Constant.CONNECTION_UUID);
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private ConnectedThread connectedThread;

    public ClientThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter, Handler handler) {
        this.device = device;
        this.bluetoothAdapter = bluetoothAdapter;
        this.handler = handler;
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Message message=handler.obtainMessage(Constant.MSG_ERROR,"客户端创建失败");
            handler.sendMessage(message);
        }
    }

    @Override
    public void run() {
        //取消客户端的搜索，因为在连接的时候开启搜索会很慢
        bluetoothAdapter.cancelDiscovery();
        try{
             socket.connect();
        }catch (IOException connectException){
            //如果该蓝牙设备的服务端没有开启，则会启动下面的代码，报错
            Message message=handler.obtainMessage(Constant.MSG_ERROR,"组员："+device.getName()+"离线");
            handler.sendMessage(message);
                cancel();
                return;
        }
        manageConnectdSocket(socket);
    }
    private  void manageConnectdSocket(BluetoothSocket socket){
        Message message=handler.obtainMessage(Constant.MSG_CONNECTION_TO_SERVER,"组员："+device.getName()+"在线");
        handler.sendMessage(message);
        connectedThread=new ConnectedThread(socket,handler);
        connectedThread.start();
    }

    /**
     * 客户端发送消息给服务端
     */
    public  void sendData(byte[]data){
        if(connectedThread!=null){
            connectedThread.write(data);
        }
    }
    /**
     * 关闭客户端
     */
    public  void cancel(){
        try{
            socket.close();
        }catch (IOException e){
            Message message=handler.obtainMessage(Constant.MSG_ERROR,"客户端关闭失败");
            handler.sendMessage(message);
        }
    }
}

