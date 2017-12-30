package com.gizwits.opensource.appkit.ControlModule;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.opensource.appkit.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class GosInitialUI_LK extends GosControlModuleBaseActivity {
    private TextView tvTime;
    private GizWifiDevice mDevice;
    private TextView nextTime1;
    private TextView nextTime2;
    private TextView nextTime3;
    private TextView nextTime4;
    private TextView nextTime5;

    private enum handler_key {

        /** 更新界面 */
        UPDATE_UI,

        DISCONNECT,
    }
    public static final int MSG_ONE = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //通过消息的内容msg.what  分别更新ui
            switch (msg.what) {
                case MSG_ONE:
                    //获取到系统当前时间 long类型
                    long time = System.currentTimeMillis();
                    //将long类型的时间转换成日历格式
                    Date data = new Date(time);
                    // 转换格式，年月日时分秒 星期  的格式
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 EEE");
                    //显示在textview上，通过转换格式
                    tvTime.setText(simpleDateFormat.format(data));
                    break;
                default:
                    break;
            }
        }
    };
    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (isDeviceCanBeControlled()) {
                progressDialog.cancel();
            } else {
                toastDeviceNoReadyAndExit();
            }
        }

    };

    /** The handler. */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            handler_key key = handler_key.values()[msg.what];
            switch (key) {
                case UPDATE_UI:
                    updateUI();
                    break;
                case DISCONNECT:
                    toastDeviceDisconnectAndExit();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lk_time_selection);
        initDevice();
        initView();
        tvTime=(TextView)findViewById(R.id.tvtime);
        new TimeThread().start();
    }
    public class TimeThread extends Thread {
        //重写run方法
        @Override
        public void run() {
            super.run();
            // do-while  一 什么什么 就
            do {
                try {
                    //每隔一秒 发送一次消息
                    Thread.sleep(1000);
                    Message msg = new Message();
                    //消息内容 为MSG_ONE
                    msg.what = MSG_ONE;
                    //发送
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    private void initView() {

        nextTime1=(TextView)findViewById(R.id.nextText1);
        nextTime2=(TextView)findViewById(R.id.nextText2);
        nextTime3=(TextView)findViewById(R.id.nextText3);
        nextTime4=(TextView)findViewById(R.id.nextText4);
        nextTime5=(TextView)findViewById(R.id.nextText5);
    }
    protected void updateUI() {

     if(data_Remaining_Pack==0) {
         nextTime1.setVisibility(View.VISIBLE);
         nextTime2.setVisibility(View.INVISIBLE);
     }
        if(data_Remaining_Pack==1) {
            nextTime1.setVisibility(View.INVISIBLE);
            nextTime2.setVisibility(View.VISIBLE);
        }
        if(data_Remaining_Pack==2) {
            nextTime2.setVisibility(View.INVISIBLE);
            nextTime3.setVisibility(View.VISIBLE);
        }
    }
    private void toastDeviceDisconnectAndExit() {
        Toast.makeText(GosInitialUI_LK.this, "连接已断开", Toast.LENGTH_SHORT).show();
        finish();
    }
    private void initDevice() {
        Intent intent = getIntent();
        mDevice = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
        mDevice.setListener(gizWifiDeviceListener);
        Log.i("Apptest", mDevice.getDid());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
        // 退出页面，取消设备订阅
        mDevice.setSubscribe(false);
        mDevice.setListener(null);
    }
    private boolean isDeviceCanBeControlled() {
        return mDevice.getNetStatus() == GizWifiDeviceNetStatus.GizDeviceControlled;
    }
    private void toastDeviceNoReadyAndExit() {
        Toast.makeText(this, "设备无响应，请检查设备是否正常工作", Toast.LENGTH_SHORT).show();
        finish();
    }
    @Override
    protected void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
        super.didUpdateNetStatus(device, netStatus);
        if (netStatus == GizWifiDeviceNetStatus.GizDeviceControlled) {
            mHandler.removeCallbacks(mRunnable);
            progressDialog.cancel();
        } else {
            mHandler.sendEmptyMessage(handler_key.DISCONNECT.ordinal());
        }
    }
    @Override
    protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device, ConcurrentHashMap<String, Object> dataMap, int sn) {
        super.didReceiveData(result, device, dataMap, sn);
        Log.i("liang", "接收到数据");
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS && dataMap.get("data") != null) {
            getDataFromReceiveDataMap(dataMap);
            mHandler.sendEmptyMessage(handler_key.UPDATE_UI.ordinal());
        }
    }
}
