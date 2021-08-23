package com.google.blockly.android.demo.robot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.blockly.android.demo.R;
import com.google.blockly.android.demo.bleutils.BleController;
import com.google.blockly.android.demo.bleutils.callback.OnWriteCallback;
import com.google.blockly.android.demo.config.Config;
import com.google.blockly.util.ToastUtils;
import com.kongqw.rockerlibrary.view.RockerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * created by jafir on 2018/4/6
 */
public class RobotControlActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final String TAG = "RobotControlActivity";
    private BleController mBleController;
    private long lastTime;

    public static void launch(Context context) {
        context.startActivity(new Intent(context, RobotControlActivity.class));
    }

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.layout_control)
    View controlLayout;

    @BindView(R.id.rockerView)
    RockerView rockerView;

    @BindView(R.id.start_and_pause)
    ImageView startAndPause;

    @BindView(R.id.introduce)
    TextView introduce;

    @BindView(R.id.control)
    View control;
    @BindView(R.id.follow)
    View xunji;
    @BindView(R.id.hide)
    View hide;
    @BindView(R.id.prevent_down)
    View prevent;


    @BindView(R.id.go)
    View mGo;
    @BindView(R.id.back)
    View mBack;
    @BindView(R.id.left)
    View mLeft;
    @BindView(R.id.right)
    View mRight;
    @BindView(R.id.stop)
    View mStop;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //开始  bắt đầu
        if (!mBleController.isConnected()) {
            RobotBleConnectActivity.launch(this);
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && System.currentTimeMillis() - lastTime < Config.sleepTime) {
            return false;
        }
        lastTime = System.currentTimeMillis();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            v.setPressed(true);
            switch (v.getId()) {
                case R.id.go:
                    //todo 前进 đi tới
                    sendCommand(CommandConstant.GO);
                    break;
                case R.id.left:
                    sendCommand(CommandConstant.LEFT);
                    //todo 向左 rẽ trái
                    break;
                case R.id.right:
                    sendCommand(CommandConstant.RIGHT);
                    //todo 向右 rẽ phải
                    break;
                case R.id.back:
                    sendCommand(CommandConstant.BACK);
                    //todo 后退 đi lùi
                    break;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            v.setPressed(false);
            sendCommand(CommandConstant.STOP);
        }
        return true;
    }

    @OnClick(R.id.stop)
    public void onStopClick() {
        //开始 bắt đầu
        if (!mBleController.isConnected()) {
            RobotBleConnectActivity.launch(this);
            return;
        }
        if (System.currentTimeMillis() - lastTime < Config.sleepTime) {
            return;
        }
        lastTime = System.currentTimeMillis();
        sendCommand(CommandConstant.STOP);
    }

    enum Mode {
        Control,//控制 điều khiển
        Follow,//循迹 Theo dõi
        Hide,//避障 Tránh
        Prevent//防跌落 Chống rơi
    }

    Mode mode = Mode.Control;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //设置当前窗体为全屏显示  full màn hình
        getWindow().setFlags(flag, flag);
        setContentView(R.layout.activity_control);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        mBleController = BleController.getInstance();
        mBleController.RegistReciveListener(TAG, value -> Log.e("response", new String(value)));
        controlLayout.setVisibility(View.GONE);
        control(Mode.Follow);
        mGo.setOnTouchListener(this);
        mBack.setOnTouchListener(this);
        mLeft.setOnTouchListener(this);
        mRight.setOnTouchListener(this);

        rockerView.setCallBackMode(RockerView.CallBackMode.CALL_BACK_MODE_STATE_CHANGE);
        rockerView.setOnShakeListener(RockerView.DirectionMode.DIRECTION_8, new RockerView.OnShakeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void direction(RockerView.Direction direction) {
                switch (direction){
                    case DIRECTION_UP:
                        sendCommand(CommandConstant.GO);
                        break;
                    case DIRECTION_DOWN:
                        sendCommand(CommandConstant.BACK);
                        break;
                    case DIRECTION_LEFT:
                        sendCommand(CommandConstant.LEFT);
                        break;
                    case DIRECTION_RIGHT:
                        sendCommand(CommandConstant.RIGHT);
                        break;
                    case DIRECTION_UP_LEFT:
                        sendCommand(CommandConstant.LEFT_GO);
                        break;
                    case DIRECTION_DOWN_LEFT:
                        sendCommand(CommandConstant.LEFT_BACK);
                        break;
                    case DIRECTION_UP_RIGHT:
                        sendCommand(CommandConstant.RIGHT_GO);
                        break;
                    case DIRECTION_DOWN_RIGHT:
                        sendCommand(CommandConstant.RIGHT_BACK);
                        break;
                }
            }

            @Override
            public void onFinish() {
                sendCommand(CommandConstant.STOP);
            }
        });
    }

    @OnClick(R.id.to_back)
    public void back() {
        finish();
    }

    @OnClick({R.id.control, R.id.follow, R.id.hide, R.id.prevent_down})
    public void click(View v) {
        switch (v.getId()) {
            case R.id.control:
                control(Mode.Control);
                break;
            case R.id.follow:
                control(Mode.Follow);
                break;
            case R.id.hide:
                control(Mode.Hide);
                break;
            case R.id.prevent_down:
                control(Mode.Prevent);
                break;
        }
    }

    private void control(Mode mode) {
        if (this.mode != mode && startAndPause.isSelected()) {
            ToastUtils.show("请先停止，再切换其他选项");
            return;
        }
        this.mode = mode;
        startAndPause.setVisibility(View.VISIBLE);
        control.setSelected(false);
        xunji.setSelected(false);
        controlLayout.setVisibility(View.GONE);
        hide.setSelected(false);
        prevent.setSelected(false);
        switch (mode) {
            case Control:
                control.setSelected(true);
                imageView.setVisibility(View.GONE);
                introduce.setVisibility(View.GONE);
                controlLayout.setVisibility(View.VISIBLE);
                rockerView.setVisibility(View.GONE);
                startAndPause.setImageResource(R.drawable.select_start_pause_yellow);
                break;
            case Follow:
                xunji.setSelected(true);
                imageView.setImageResource(R.mipmap.xunji);
                imageView.setVisibility(View.VISIBLE);
                introduce.setVisibility(View.VISIBLE);
                introduce.setText("循迹");
                startAndPause.setImageResource(R.drawable.select_start_pause_blue);
                break;
            case Hide:
                hide.setSelected(true);
                imageView.setImageResource(R.mipmap.bizhang);
                imageView.setVisibility(View.VISIBLE);
                introduce.setVisibility(View.VISIBLE);
                introduce.setText("避障");
                startAndPause.setImageResource(R.drawable.select_start_pause_red);
                break;
            case Prevent:
                prevent.setSelected(true);
                imageView.setImageResource(R.mipmap.fangdieluo);
                imageView.setVisibility(View.GONE);
                introduce.setVisibility(View.GONE);
                controlLayout.setVisibility(View.GONE);
                rockerView.setVisibility(View.VISIBLE);
                introduce.setText("摇杆");
                startAndPause.setImageResource(R.drawable.select_start_pause_green);
                break;
        }

    }

    @OnClick(R.id.start_and_pause)
    public void startAndPause(View view) {
        //selected   true 为暂停  false为开始
        if (System.currentTimeMillis() - lastTime < 1000) {
            return;
        }
        lastTime = System.currentTimeMillis();
        //开始
        if (!mBleController.isConnected()) {
            RobotBleConnectActivity.launch(this);
            return;
        }

        if (mode != Mode.Control && mode != Mode.Prevent) {
            view.setSelected(!view.isSelected());
        }
        if (view.isSelected()) {
            switch (mode) {
                case Follow:
                    //todo 循迹
                    sendCommand(CommandConstant.XUNJI_START);
                    break;
                case Hide:
                    //todo 避障
                    sendCommand(CommandConstant.BIZHANG_START);
                    break;
//                case Prevent:
//                    sendCommand(CommandConstant.FANGDIELUO_START);
//                    //todo 防跌落
//                    break;
            }
        } else {
            //暂停
            switch (mode) {
                case Follow:
                    //todo 循迹
                    sendCommand(CommandConstant.XUNJI_END);
                    break;
                case Hide:
                    //todo 避障
                    sendCommand(CommandConstant.BIZHANG_END);
                    break;
//                case Prevent:
//                    sendCommand(CommandConstant.FANGDIELUO_END);
//                    //todo 防跌落
//                    break;
            }
        }
    }

    @SuppressLint("CheckResult")
    private void sendCommand(String command) {
        Log.e(TAG, "send data：" + command);
        if (mBleController.isConnected()) {
            mBleController.WriteBuffer(command, new OnWriteCallback() {
                @Override
                public void onSuccess() {
                    Log.e("debug", "onSuccess");
                }

                @Override
                public void onFailed(int state) {
                    ToastUtils.show("发送失败");
                    Log.e("debug", "onFailed" + state);
                }
            });
        }
    }
//
//    @OnTouch({R.id.go, R.id.left, R.id.stop, R.id.right, R.id.back})
//    public boolean controlOnTouch(View v, MotionEvent event) {
//
//        return true;
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleController.UnregistReciveListener(TAG);
    }
}
