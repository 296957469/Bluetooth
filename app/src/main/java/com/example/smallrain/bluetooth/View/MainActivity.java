package com.example.smallrain.bluetooth.View;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.smallrain.bluetooth.Controller.BlueToothController;
import com.example.smallrain.bluetooth.R;
import com.example.smallrain.bluetooth.Controller.connect.ServerSocketThread;
import com.example.smallrain.bluetooth.Controller.connect.ClientThread;
import com.example.smallrain.bluetooth.Controller.connect.Constant;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //请求码
    private static final int REQUEST_CODE = 0;
    //蓝牙工具类
    private BlueToothController controller = new BlueToothController();
    //获取到已经绑定的设备存放到容器
    private List<BluetoothDevice> boundedDeviceList = new ArrayList<>();
    //封装在showToast函数的全局变量，便于输出消息
    private Toast toast;
    //布局视图&适配器&存放设备的容器
    private  ListView listView;
    private DeviceAdapter adapter;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    //权限请求码，安卓手机6.0以上只有同意这个权限才能接受到搜索到设备的广播
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    //服务端
    private ServerSocketThread serverSocketThread;
    //客户端
    private ClientThread clientThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      // initActionBar();
        setContentView(R.layout.activity_main);
        initUI();
        //广播的过滤器
        IntentFilter filter = new IntentFilter();
        //开始查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //结束查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //查找设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //设备扫描模式改变
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //绑定状态
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //注册广播
        registerReceiver(receiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }
    private Handler mUIHandler=new MyHandler();
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                }
                break;
        }
    }
    //监听蓝牙的广播
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            //1.开始查找
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                setProgressBarIndeterminate(true);
                //初始化数据列表
                deviceList.clear();
                adapter.notifyDataSetChanged();
                showToast("开始查找");
            }
            //2.结束查找
           if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                setProgressBarIndeterminate(false);
                showToast("结束查找");
            }
            //3.查找设备
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
               BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //找到一个，添加一个
                deviceList.add(device);
                adapter.notifyDataSetChanged();

            }
            //4.设备扫描模式改变
            if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
            {
                int scanMode=intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,0);
                if(scanMode==BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                   // setProgressBarIndeterminate(true);
                }
                else{
                  //  setProgressBarIndeterminate(false);
                }
            }
            //5.绑定状态
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                BluetoothDevice remoteDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(remoteDevice==null){
                    showToast("no device");
                    return;
                }
                int status=intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if(status==BluetoothDevice.BOND_BONDED){
                    showToast("Bonded "+remoteDevice.getName());
                }
                else if(status==BluetoothDevice.BOND_BONDING){
                    showToast("Bonding "+remoteDevice.getName());
                }
                else if(status==BluetoothDevice.BOND_NONE){
                    showToast("Not bond "+remoteDevice.getName());
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serverSocketThread !=null){
            serverSocketThread.cancel();
        }
        if(clientThread !=null){
            clientThread.cancel();
        }
        unregisterReceiver(receiver);
    }
    //初始化listview
    private void initUI() {
        listView=(ListView)findViewById(R.id.device_list);
        adapter=new DeviceAdapter(deviceList,this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(bindDeviceClick);
    }
    //监听搜索设备页面lisview的点击动作，点击代表与该设备建立一个配对请求连接
    private AdapterView.OnItemClickListener bindDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            BluetoothDevice device = deviceList.get(i);
            device.createBond();
        }
    };
    //监听已绑定设备页面lisview的点击动作，点击代表与该设备进行通信
    private AdapterView.OnItemClickListener bindedDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            BluetoothDevice device = boundedDeviceList.get(i);
            //如果想连接多个设备，取消下面的if语句
            if( clientThread != null) {
                clientThread.cancel();
            }
            clientThread = new ClientThread(device, controller.getAdapter(), mUIHandler);
            clientThread.start();
        }
    };
    //初始化标题栏
    private void initActionBar() {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getActionBar().setDisplayUseLogoEnabled(false);
        setProgressBarIndeterminate(true);
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //重写方法，创建选项菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       getMenuInflater().inflate(R.menu.menu_main,menu);
       return true;
    }
    //重写方法，监听选项菜单的点击动作
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id=item.getItemId();
        if(id==R.id.turn_on_bluetooth)
        {
            //开启蓝牙
            controller.turnOnBlueTooth(this,REQUEST_CODE);
        }
        else if(id==R.id.turn_off_bluetooth)
        {
            //关闭蓝牙
            controller.turnOffBlueTooth();
            showToast("蓝牙已经关闭");
        }
        else if(id==R.id.enable_visiblity)
        {
            //开启设备的可见性
            controller.enableVisibly(this);
        }
        else if(id==R.id.find_device){
            //扫描其他设备
            adapter.refresh(deviceList);
            controller.findDevice();
            listView.setOnItemClickListener(bindDeviceClick);
        }
        else if(id==R.id.bonded_device){
            //查看已经绑定的设备
            boundedDeviceList=controller.getBoundedDeviceList();
            adapter.refresh(boundedDeviceList);
            listView.setOnItemClickListener(bindedDeviceClick);
        }
        else if(id==R.id.listening){
            //开启服务端
           if(serverSocketThread !=null){
               serverSocketThread.cancel();
           }
           serverSocketThread =new ServerSocketThread(controller.getAdapter(),mUIHandler);
           serverSocketThread.start();
        }
        else if(id==R.id.stop_listening){
            //关闭服务端
            if(serverSocketThread !=null){
                serverSocketThread.cancel();
            }
        }
        else if(id==R.id.disconnect){
            //断开所有连接
            if(clientThread !=null){
                clientThread.cancel();
            }
        }
        else if(id==R.id.say_hello){
          say("Hello");
        }
        else if(id==R.id.say_hi){
            say("Hi");
        }
        return super.onOptionsItemSelected(item);
    }
    private void say(String word){
        //如果是想实现组播，这里要使用一个容器存放每一个serverSocketThread （组员）然后遍历这个容器，组播出去
        //并且改变serverSocketThread的run方法，连接一个设备的时候，服务器继续运行打开
        if(serverSocketThread !=null) {
            try {
                serverSocketThread.sendData(word.getBytes("utf-8"));
            } catch (UnsupportedEncodingException e) {

            }
        }
        //如果是想实现组播，这里要使用一个容器存放每一个clientThread（组员）然后遍历这个容器，组播出去
            else if(clientThread !=null){
                try{
                    clientThread.sendData(word.getBytes("utf-8"));
            }catch (UnsupportedEncodingException e){

                }
        }
    }
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.MSG_START_LISTENING:
                    showToast("开始监听 ");
                   // setProgressBarIndeterminateVisibility(true);
                    break;
                case Constant.MSG_FINISH_LISTENING:
                    showToast("监听结束");
                   // setProgressBarIndeterminateVisibility(false);
                    break;
                case Constant.MSG_GOT_DATA:
                    showToast("data: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_ERROR:
                    showToast("error: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_CONNECTION_TO_SERVER:
                    showToast("Connected to Server");
                    break;
                case Constant.MSG_GOT_A_CLIENT:
                    showToast("Got a Client");
                    break;
            }
        }
    }
    /**
     * 封装好的Toast
     * @param text
     */
    private void showToast(String text)
    {
        if(toast ==null)
        {
            toast =Toast.makeText(this,text,Toast.LENGTH_SHORT);
        }
        else
        {
            toast.setText(text);
        }
        toast.show();
    }
}
