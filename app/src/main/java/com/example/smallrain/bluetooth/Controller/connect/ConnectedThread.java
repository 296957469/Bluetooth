package com.example.smallrain.bluetooth.Controller.connect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**基础连接线程
 * 无论是服务端还是客户端
 * 他们连接之后的socket都丢到这个线程里面来处理
 * Created by SmallRain on 2018/4/29.
 */

public class ConnectedThread extends Thread {
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;


    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
        try {
            inputStream = socket.getInputStream();
            outputStream=socket.getOutputStream();
        }catch (Exception e){
            handler.sendMessage(handler.obtainMessage(Constant.MSG_ERROR,socket.getRemoteDevice().getName()+"异常"));
            cancel();
        }
    }
    @Override
    public void run() {
        byte[] buffer=new byte[1024];
        int len;
        while(true){
            try{
                len=inputStream.read(buffer);
                if (len>0){
                    Message message=handler.obtainMessage(Constant.MSG_GOT_DATA,new String(buffer,0,len,"utf-8"));
                    handler.sendMessage(message);
                }
            }catch (IOException e){
                   handler.sendMessage(handler.obtainMessage(Constant.MSG_ERROR,socket.getRemoteDevice().getName()+"离线"));
                   cancel();
                   break;
            }
        }
    }
    /* Call this from the main activity to send data to the remote device */
    public  void write(byte[]bytes){
        try{
            outputStream.write(bytes);
        }catch (IOException e){
            handler.sendMessage(handler.obtainMessage(Constant.MSG_ERROR,socket.getRemoteDevice().getName()+"离线"));
            cancel();
        }
    }
    /* Call this from the main activity to shutdown the connection */
    private void cancel(){
        try{
            socket.close();
        }catch (Exception e){
            handler.sendMessage(handler.obtainMessage(Constant.MSG_ERROR,socket.getRemoteDevice().getName()+"关闭失败"));
            cancel();
        }
    }
















}
