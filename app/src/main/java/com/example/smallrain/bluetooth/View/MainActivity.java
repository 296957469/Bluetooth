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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.smallrain.bluetooth.Controller.BlueToothController;
import com.example.smallrain.bluetooth.R;
import com.example.smallrain.bluetooth.Controller.connect.ServerSocketThread;
import com.example.smallrain.bluetooth.Controller.connect.ClientThread;
import com.example.smallrain.bluetooth.Controller.connect.Constant;

import java.io.UnsupportedEncodingException;
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
    //存放多个clientThread的容器，实现一个设备可以连接多个设备，实现组播功能
    private ArrayList<ClientThread>clientThreadArrayList=new ArrayList<>();
    //消息通知
    private Handler mUIHandler=new MyHandler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        //权限获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        //如果打开应用之前，蓝牙是已经开启的,则自动开启服务端
         if(controller.getBlueToothStatus()){
             serverSocketThread =new ServerSocketThread(controller.getAdapter(),mUIHandler);
             serverSocketThread.start();
         }
    }

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
                    showToast("组员不存在");
                    return;
                }
                int status=intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if(status==BluetoothDevice.BOND_BONDED){
                    showToast("添加组员成功 ");
                    //每次新添加一个组员，就断开所有连接，清空容器，重新连接所有连接 因为有新成员的加入
                    allCancle();
                }
                else if(status==BluetoothDevice.BOND_BONDING){
                    showToast("发送验证......... ");
                }
                else if(status==BluetoothDevice.BOND_NONE){
                    showToast("组员添加失败 ");
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭服务端
        if(serverSocketThread !=null){
            serverSocketThread.cancel();
        }
        //断开所有主动发起的连接
        allCancle();
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
            controller.cancelDiscovery();//请求连接的时候，取消设备的搜索
            device.createBond();
        }
    };
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
            //先关闭服务端，避免抛异常
            if(controller.getBlueToothStatus())
            {
              if (serverSocketThread != null){
                    serverSocketThread.cancel();
                    serverSocketThread=null;
                }
                allCancle();
               controller.turnOffBlueTooth();
            }
          // showToast("蓝牙已经关闭");
        }
        else if(id==R.id.enable_visiblity)
        {
            //开启设备的可见性
            controller.enableVisibly(this);
        }
        else if(id==R.id.find_device){
            //扫描其他设备
            if(controller.getBlueToothStatus()){
                controller.cancelDiscovery();
                deviceList.clear();
            adapter.refresh(deviceList);
            controller.startDiscovery();
            listView.setOnItemClickListener(bindDeviceClick);
        }else
            {
                showToast("请先打开蓝牙");
            }
        }
        else if(id==R.id.bonded_device){
            //查看已经绑定的设备
            if(controller.getBlueToothStatus()){
            boundedDeviceList=controller.getBoundedDeviceList();
            adapter.refresh(boundedDeviceList);
            listView.setOnItemClickListener(null);
        }else{
                showToast("请先打开蓝牙");
            }

        }
        else if(id==R.id.say_hello){
            if(controller.getBlueToothStatus()){
                say("Hello");
            }
            else{
                showToast("请先打开蓝牙");
            }

        }
        else if(id==R.id.say_hi){
            if(controller.getBlueToothStatus()){
                say("Hi");
            }
            else{
                showToast("请先打开蓝牙");
            }
        }
        return super.onOptionsItemSelected(item);
    }
    //后期优化，下面代码的缺点是：每次组播要和组员一个个的建立连接，很耗费时间
    private void say(String word) {
        //实现组播，组员不能为空才可以进行组播
        int len=controller.getBoundedDeviceList().size();
        if(len>0) {
            //如果已经和所有组员建立好了连接，则直接进行组播
            if(clientThreadArrayList.size()>0){
                for (ClientThread temp : clientThreadArrayList) {
                    try {
                        temp.sendData(word.getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {

                    }
                }
            }
            else{
                //先和所有组员建立连接
                for (BluetoothDevice device : controller.getBoundedDeviceList()) {
                    clientThread = new ClientThread(device, controller.getAdapter(), mUIHandler);
                    clientThread.start();
                    clientThreadArrayList.add(clientThread);
                }
                //然后再实现组播
                // 缺点：有些连接并没有真正的连接上，但是发送消息的时候也被调用来发送消息
                for (ClientThread temp : clientThreadArrayList) {
                    try {
                        temp.sendData(word.getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {

                    }
                }

            }
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.MSG_START_LISTENING:
                   // showToast("开始监听 ");
                   // setProgressBarIndeterminateVisibility(true);
                    break;
                case Constant.MSG_FINISH_LISTENING:
                    //showToast("监听结束");
                   // setProgressBarIndeterminateVisibility(false);
                    break;
                case Constant.MSG_GOT_DATA:
                    showToast("Data: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_ERROR:
                    showToast("Error: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_CONNECTION_TO_SERVER:
                    showToast("Connected to Server "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_GOT_A_CLIENT:
                    showToast("Got a Client "+String.valueOf(msg.obj));
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //当发起请求蓝牙打开事件时，会告诉你用户选择的结果
        if (resultCode == RESULT_OK) {
            //showToast("打开成功");
            //蓝牙一旦开启成功，自动开启服务端等待客户端的连接
            if (serverSocketThread == null) {
                serverSocketThread = new ServerSocketThread(controller.getAdapter(), mUIHandler);
                serverSocketThread.start();
            }

        } else {
            showToast("打开失败");
        }
    }
    //断开自己主动发起的所有连接,并且清空clientThreadArrayList容器
    private void allCancle(){
        if(clientThreadArrayList.size()>0){
            for(ClientThread temp:clientThreadArrayList)
            {
                temp.cancel();
            }
            clientThreadArrayList.clear();
        }
    }
}
