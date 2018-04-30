package com.example.smallrain.bluetooth.Controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**蓝牙工具类，封装了所有对蓝牙的操作
 * Created by SmallRain on 2018/4/28.
 */

public class BlueToothController {
    private BluetoothAdapter adapter;

    public BlueToothController() {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * 1.判断是否支持蓝牙
     * @return true 支持 false 不支持
     */
    public boolean isSupportBlueTooth() {
        if(adapter !=null)
            return true;
        else
            return false;
    }

    /**
     * 2.判断当前蓝牙状态
     * @return true 打开 false 关闭
     */
    public boolean getBlueToothStatus(){
        assert (adapter !=null);//如果设备不支持蓝牙，程序崩溃
        return adapter.isEnabled();
    }
    /**
     * 3.打开蓝牙
     * @param  activity
     * @param requestCode
     */
    public void turnOnBlueTooth(Activity activity,int requestCode){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent,requestCode);
    }
    /**
     * 4.关闭蓝牙
     */
    public void turnOffBlueTooth(){
        adapter.disable();
    }
    /**
     * 5.打开蓝牙的可见性
     * @param context
     */
    public void enableVisibly(Context context){
        Intent discoverableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        context.startActivity(discoverableIntent);
    }
    /**
     * 6.查找设备
     */
    public void findDevice()
    {
        assert (adapter!=null);
        adapter.startDiscovery();
    }
    /**
     * 7.获取绑定设备
     */
    public List<BluetoothDevice>getBoundedDeviceList(){
        return new ArrayList<>(adapter.getBondedDevices());
    }
}

