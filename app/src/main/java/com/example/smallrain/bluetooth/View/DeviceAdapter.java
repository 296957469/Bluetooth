package com.example.smallrain.bluetooth.View;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**自定义的设备适配器
 * Created by SmallRain on 2018/4/28.
 */

public class DeviceAdapter extends BaseAdapter {
   private List<BluetoothDevice>data;
   private Context context;

    public DeviceAdapter(List<BluetoothDevice> data, Context context) {
        this.data = data;
        this.context = context.getApplicationContext();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View itemVIew=view;
        //复用View，优化性能
        if(itemVIew==null){
            itemVIew= LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2,viewGroup,false);
        }
        TextView line1=(TextView)itemVIew.findViewById(android.R.id.text1);
        TextView line2=(TextView)itemVIew.findViewById(android.R.id.text2);
        //获取对应的蓝牙设备
        BluetoothDevice device=(BluetoothDevice)getItem(i);
        //显示名称
        line1.setText(device.getName());
        line1.setTextColor(Color.BLACK);//修改字体颜色，默认为白色，改为黑色
        //显示地址
        line2.setText(device.getAddress());
        line2.setTextColor(Color.BLACK);
        return itemVIew;
    }
    //刷新该适配器对应的listview
    public  void refresh(List<BluetoothDevice>data){
        this.data=data;
        notifyDataSetChanged();
    }
}