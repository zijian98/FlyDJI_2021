package com.dji.flydji_new;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

public class FlightActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView mTakeoff, mLanding, mUP, mDOWN, mLEFT, mRIGHT, mFORWARD, mBACKWARD, mSTOP, mRollRight, mRollLeft;
    private TextView mVelX, mVelY, mVelZ, mAltitude, mFlightMode;
    private FlightMode FM;
    private float X, Y, Z;
    private double ALT;
    private FlightController flightController;

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

        initUI();

        //Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FlyDJI.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

    }

    private void initUI() {
        mTakeoff = findViewById(R.id.takeoff_card);
        mLanding = findViewById(R.id.landing_card);
        mUP = findViewById(R.id.throttleUP_card);
        mDOWN = findViewById(R.id.throttleDOWN_card);
        mLEFT = findViewById(R.id.yawLEFT_card);
        mRIGHT = findViewById(R.id.yawRIGHT_card);
        mFORWARD =findViewById(R.id.pitchFORWARD_card);
        mBACKWARD = findViewById(R.id.pitchBACKWARD_card);
        mSTOP = findViewById(R.id.STOP_card);
        mRollRight = findViewById(R.id.rollRight_card);
        mRollLeft = findViewById(R.id.rollLeft_card);

        mVelX = findViewById(R.id.txt_velX);
        mVelY = findViewById(R.id.txt_velY);
        mVelZ = findViewById(R.id.txt_velZ);
        mAltitude = findViewById(R.id.txt_altitude);
        mFlightMode = findViewById(R.id.txt_flightMode);

        mTakeoff.setOnClickListener(this);
        mLanding.setOnClickListener(this);

        mUP.setOnClickListener(this);
        mDOWN.setOnClickListener(this);
        mLEFT.setOnClickListener(this);
        mRIGHT.setOnClickListener(this);
        mFORWARD.setOnClickListener(this);
        mBACKWARD.setOnClickListener(this);
        mSTOP.setOnClickListener(this);
        mRollRight.setOnClickListener(this);
        mRollLeft.setOnClickListener(this);
    }

    private void initFlightController() {

        BaseProduct product = FlyDJI.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }

        if(flightController != null)
        {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    X = flightControllerState.getVelocityX();
                    Y = flightControllerState.getVelocityY();
                    Z = flightControllerState.getVelocityZ();
                    ALT = flightControllerState.getAircraftLocation().getAltitude();
                    FM = flightControllerState.getFlightMode();

                    updateDroneInfo();
                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange() {
        initFlightController();
    }

    private void updateDroneInfo()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVelX.setText("Velocity X: " + String.format("%.3f",  X) + " m/s");
                mVelY.setText("Velocity Y: " + String.format("%.3f",  Y) + " m/s");
                mVelZ.setText("Velocity Z: " + String.format("%.3f",  Z) + " m/s");
                mAltitude.setText("Altitude: " + String.format("%.3f",  ALT) + " m");
                mFlightMode.setText(String.valueOf(FM));
            }
        });
    }

    @Override
    public void onClick(View v) {

        if(flightController == null)
        {
            showToast("Disconnected!");
        }
        else
        {
            if(mTakeoff.isPressed())
            {
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError == null)
                        {
                            showToast("Taking off!");
                        }
                        else
                        {
                            showToast(djiError.getDescription());
                        }
                    }
                });
            }
            if(mLanding.isPressed())
            {
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError == null)
                        {
                            showToast("Landing!");
                            flightController.setStateCallback(new FlightControllerState.Callback() {
                                @Override
                                public void onUpdate(FlightControllerState flightControllerState) {
                                    if(flightControllerState.isLandingConfirmationNeeded())
                                    {
                                        flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if(djiError == null)
                                                {
                                                    showToast("Confirm Landing");
                                                }
                                                else
                                                {
                                                    showToast(djiError.getDescription());
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
            if(mUP.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                        showToast("Throttle up");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 0, 0, 3),
                                    new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    });
                            if(mSTOP.isPressed() || mBACKWARD.isPressed() || mLanding.isPressed() ||
                                    mLEFT.isPressed() || mRIGHT.isPressed() || mFORWARD.isPressed() || mDOWN.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mDOWN.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                        showToast("Throttle down");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 0, 0, -3),
                                    new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    });
                            if(mSTOP.isPressed() || mBACKWARD.isPressed() || mLanding.isPressed() ||
                                    mLEFT.isPressed() || mRIGHT.isPressed() || mUP.isPressed() || mFORWARD.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mLEFT.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                        showToast("Yaw left");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 0, -10, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                            if(mSTOP.isPressed() || mBACKWARD.isPressed() || mLanding.isPressed() ||
                                    mFORWARD.isPressed() || mRIGHT.isPressed() || mDOWN.isPressed() || mUP.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mRIGHT.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                        showToast("Yaw right");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 0, 10, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                            if(mSTOP.isPressed() || mBACKWARD.isPressed() || mLanding.isPressed() ||
                                    mLEFT.isPressed() || mFORWARD.isPressed() || mDOWN.isPressed() || mUP.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mRollRight.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                        showToast("Pitch forward");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(3, 0, 0, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                            if(mSTOP.isPressed() || mBACKWARD.isPressed() || mLanding.isPressed() ||
                                    mLEFT.isPressed() || mRIGHT.isPressed() || mDOWN.isPressed() || mUP.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mRollLeft.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                        showToast("Pitch backward");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(-3, 0, 0, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                            if(mSTOP.isPressed() || mFORWARD.isPressed() || mLanding.isPressed() ||
                                    mLEFT.isPressed() || mRIGHT.isPressed() || mDOWN.isPressed() || mUP.isPressed())
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mFORWARD.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                        showToast("Pitch forward");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 3, 0, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                            if(!(mFORWARD.isPressed()))
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mBACKWARD.isPressed())
            {
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                        showToast("Pitch backward");
                        while (flightController.isVirtualStickControlModeAvailable())
                        {
                            flightController.sendVirtualStickFlightControlData(new FlightControlData(0, -3, 0, 0),
                                    new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    });
                            if(!(mBACKWARD.isPressed()))
                            {
                                break;
                            }
                        }
                    }
                });
            }
            if(mSTOP.isPressed())
            {
                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            }
        }


    }

    public void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }


}