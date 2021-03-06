package com.dji.flydji_new;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dji.common.error.DJIError;
import dji.common.mission.tapfly.TapFlyExecutionState;
import dji.common.mission.tapfly.TapFlyMission;
import dji.common.mission.tapfly.TapFlyMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.tapfly.TapFlyMissionEvent;
import dji.sdk.mission.tapfly.TapFlyMissionOperator;
import dji.sdk.mission.tapfly.TapFlyMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;

import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class PointingActivity extends CameraOnly implements TextureView.SurfaceTextureListener, View.OnClickListener, View.OnTouchListener {

    private static final String TAG = "PointingActivity";

    private TapFlyMission mTapFlyMission;

    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushDrawerSd;
    private Button mStartBtn;
    private ImageButton mStopBtn;
    private TextView mPushTv;
    private RelativeLayout mBgLayout;
    private ImageView mRstPointIv;
    private TextView mAssisTv;
    private Switch mAssisSw;
    private TextView mSpeedTv;
    private SeekBar mSpeedSb;

    private TapFlyMissionOperator getTapFlyOperator() {
        return DJISDKManager.getInstance().getMissionControl().getTapFlyMissionOperator();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_pointing);
        super.onCreate(savedInstanceState);
        initUI();
        getTapFlyOperator().addListener(new TapFlyMissionOperatorListener() {
            @Override
            public void onUpdate(@Nullable TapFlyMissionEvent aggregation) {
                TapFlyExecutionState executionState = aggregation.getExecutionState();
                if (executionState != null){
                    showPointByTapFlyPoint(executionState.getImageLocation(), mRstPointIv);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        initTapFlyMission();
    }

    @Override
    protected void onDestroy() {
        if(mCodecManager != null){
            mCodecManager.destroyCodec();
        }
        super.onDestroy();
    }

    private void initUI() {
        mPushDrawerIb = (ImageButton)findViewById(R.id.pointing_drawer_control_ib);
        mPushDrawerSd = (SlidingDrawer)findViewById(R.id.pointing_drawer_sd);
        mStartBtn = (Button)findViewById(R.id.pointing_start_btn);
        mStopBtn = (ImageButton)findViewById(R.id.pointing_stop_btn);
        mPushTv = (TextView)findViewById(R.id.pointing_push_tv);
        mBgLayout = (RelativeLayout)findViewById(R.id.pointing_bg_layout);
        mRstPointIv = (ImageView)findViewById(R.id.pointing_rst_point_iv);
        mAssisTv = (TextView)findViewById(R.id.pointing_assistant_tv);
        mAssisSw = (Switch)findViewById(R.id.pointing_assistant_sw);
        mSpeedTv = (TextView)findViewById(R.id.pointing_speed_tv);
        mSpeedSb = (SeekBar)findViewById(R.id.pointing_speed_sb);
        mPushDrawerIb.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);

        mSpeedSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSpeedTv.setText(progress + 1 + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getTapFlyOperator().setAutoFlightSpeed(getSpeed(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "Set Auto Flight Speed Success" : error.getDescription());
                    }
                });
            }
        });
    }

    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string) {
        PointingActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PointingActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initTapFlyMission() {
        mTapFlyMission = new TapFlyMission();
        mTapFlyMission.isHorizontalObstacleAvoidanceEnabled = mAssisSw.isChecked();
        mTapFlyMission.tapFlyMode = TapFlyMode.FORWARD;
    }

    private PointF getTapFlyPoint(View iv) {
        if (iv == null) return null;
        View parent = (View)iv.getParent();
        float centerX = iv.getLeft() + iv.getX()  + ((float)iv.getWidth()) / 2;
        float centerY = iv.getTop() + iv.getY() + ((float)iv.getHeight()) / 2;
        centerX = centerX < 0 ? 0 : centerX;
        centerX = centerX > parent.getWidth() ? parent.getWidth() : centerX;
        centerY = centerY < 0 ? 0 : centerY;
        centerY = centerY > parent.getHeight() ? parent.getHeight() : centerY;

        return new PointF(centerX / parent.getWidth(), centerY / parent.getHeight());
    }

    private void showPointByTapFlyPoint(final PointF point, final ImageView iv) {
        if (point == null || iv == null) {
            return;
        }
        final View parent = (View)iv.getParent();
        PointingActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                iv.setX(point.x * parent.getWidth() - iv.getWidth() / 2);
                iv.setY(point.y * parent.getHeight() - iv.getHeight() / 2);
                iv.setVisibility(View.VISIBLE);
                iv.requestLayout();
            }
        });
    }

    private float getSpeed() {
        if (mSpeedSb == null) return Float.NaN;
        return mSpeedSb.getProgress() + 1;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.pointing_bg_layout) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (mTapFlyMission != null) {
                        mStartBtn.setVisibility(View.VISIBLE);
                        mStartBtn.setX(event.getX() - mStartBtn.getWidth() / 2);
                        mStartBtn.setY(event.getY() - mStartBtn.getHeight() / 2);
                        mStartBtn.requestLayout();
                        mTapFlyMission.target = getTapFlyPoint(mStartBtn);
                    } else {
                        setResultToToast("TapFlyMission is null");
                    }
                    break;

                default:
                    break;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pointing_drawer_control_ib) {
            if (mPushDrawerSd.isOpened()) {
                mPushDrawerSd.animateClose();
            } else {
                mPushDrawerSd.animateOpen();
            }
            return;
        }
        if (getTapFlyOperator() != null) {
            switch (v.getId()) {
                case R.id.pointing_start_btn:
                    getTapFlyOperator().startMission(mTapFlyMission, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            setResultToToast(error == null ? "Start Mission Successfully" : error.getDescription());
                            if (error == null){
                                setVisible(mStartBtn, false);
                            }
                        }
                    });
                    break;
                case R.id.pointing_stop_btn:
                    getTapFlyOperator().stopMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            setResultToToast(error == null ? "Stop Mission Successfully" : error.getDescription());
                        }
                    });
                    break;
                default:
                    break;
            }
        } else {
            setResultToToast("TapFlyMission Operator is null");
        }
    }

    private void setVisible(final View v, final boolean visible) {
        if (v == null) return;
        PointingActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }
}