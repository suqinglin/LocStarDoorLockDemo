package com.nexless.locstar;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sziton.locstaraar.LocStarManager;
import com.sziton.locstaraar.bean.LockKeysBean;
import com.sziton.locstaraar.bean.NetBaseBean;
import com.sziton.locstaraar.bean.OpenLockResultBean;
import com.sziton.locstaraar.interfaces.IConnStatusChangeListener;
import com.sziton.locstaraar.interfaces.INetworkListener;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressDialog mDialog;
    private static final String IP = "120.76.55.235:8080";
    private static final String HOTEL_NUM = "021040";
    private static final String MOBILE = "20190601";
    private LockKeysBean mKeys;
    private Disposable mDisconnDevTimer;
    private TextView mTvGetKeysResult;
    private TextView mTvOpenDoorResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化SDK，程序开始时必须调用
        LocStarManager.getInstance().init(this, IP, HOTEL_NUM);
        LocStarManager.getInstance().setDebugMode(true);

        mTvGetKeysResult = findViewById(R.id.tv_get_keys_result);
        mTvOpenDoorResult = findViewById(R.id.tv_open_result);
        findViewById(R.id.btn_get_keys).setOnClickListener(this);
        findViewById(R.id.btn_open_door).setOnClickListener(this);
        mDialog = new ProgressDialog(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_get_keys:
                mDialog.show();
                getKeys();
                break;
            case R.id.btn_open_door:
                openDoor();
                break;
        }
    }

    /**
     * 从服务器获取到钥匙
     */
    private void getKeys() {
        LocStarManager.getInstance().getPhoneKey(MOBILE, new INetworkListener<LockKeysBean>() {
            @Override
            public void onData(LockKeysBean keys) {
                mDialog.dismiss();
                if(keys.status == 1) {
                    mKeys = keys;
                    String endTime = mKeys.endTime;
                    StringBuffer sb = new StringBuffer("有效期至:20");
                    sb.append(endTime, 0, 2).append("-");
                    sb.append(endTime, 2, 4).append("-");
                    sb.append(endTime, 4, 6).append(" ");
                    sb.append(endTime, 6, 8).append(":");
                    sb.append(endTime, 8, 10);
                    mTvGetKeysResult.setText(sb.toString());
                    mTvGetKeysResult.append("\n房号:"+mKeys.roomCode);
                } else {
                    mTvGetKeysResult.setText(keys.msg);
                }
            }

            @Override
            public void onFail(int error,String msg) {
                mDialog.dismiss();
                mTvGetKeysResult.setText("获取失败," + msg);
            }
        });
    }

    /**
     * 开锁
     */
    private void openDoor() {
        if(mKeys == null) {
            Toast.makeText(MainActivity.this,"请先获取钥匙!",Toast.LENGTH_SHORT).show();
            return;
        }
        mDialog.show();
        String hotelLockMac = splitMac(mKeys.hotelLockDevice.deviceMac);
        // 建立连接
        if (!LocStarManager.getInstance().isConnect(hotelLockMac)) {
            LocStarManager.getInstance().connDevice(hotelLockMac, hotelLockListener);
        }
        // 关闭上次的计时器
        if(mDisconnDevTimer != null && !mDisconnDevTimer.isDisposed()) {
            mDisconnDevTimer.dispose();
            mDisconnDevTimer = null;
        }
        // 向蓝牙发送开锁数据
        mKeys.hotelLockCipher.flag = "81";
        LocStarManager.getInstance().openDevice(
                splitMac(mKeys.hotelLockDevice.deviceMac),
                mKeys.hotelLockCipher,
                15,
                hotelLockListener);
    }

    private IConnStatusChangeListener hotelLockListener = new IConnStatusChangeListener() {
        @Override
        public void onData(byte[] data) {
            mDialog.dismiss();
            OpenLockResultBean resultBean = LocStarManager.getInstance().getLadderControlOpenResult(data);
            if (resultBean.isSuccess()) {
                mTvOpenDoorResult.setText("开锁成功!");
                // 同步日志到服务器
                if (resultBean.log != null) {
                    LocStarManager.getInstance().upLoadLog(resultBean.log, new INetworkListener<NetBaseBean>() {
                        @Override
                        public void onData(NetBaseBean data) {
                            Toast.makeText(MainActivity.this,data.msg,Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFail(int error, String msg) {
                            Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public void onStatusChange(int status) {
            mDialog.dismiss();
            if(status == 3) {
                mDisconnDevTimer = Observable.timer(15000,TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aLong -> {
                            if(mKeys != null)
                            {
                                LocStarManager.getInstance().disConnDevice(splitMac(mKeys.hotelLockDevice.deviceMac));
                            }
                        });
            } else if(status > 2000) {
                Toast.makeText(MainActivity.this,"开锁失败! status:"+status,Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 拆分MAC，即每两位添加一个冒号（“:”）
     * @param mac
     * @return
     */
    private String splitMac(String mac)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(mac, 0, 2).append(":");
        sb.append(mac, 2, 4).append(":");
        sb.append(mac, 4, 6).append(":");
        sb.append(mac, 6, 8).append(":");
        sb.append(mac, 8, 10).append(":");
        sb.append(mac, 10, 12);
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mKeys != null && mKeys.hotelLockDevice.deviceMac != null) {
            LocStarManager.getInstance().disConnDevice(splitMac(mKeys.hotelLockDevice.deviceMac));
        }
        if(mDisconnDevTimer != null && !mDisconnDevTimer.isDisposed()) {
            mDisconnDevTimer.dispose();
            mDisconnDevTimer = null;
        }
    }
}
