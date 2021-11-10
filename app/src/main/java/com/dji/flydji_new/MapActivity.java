package com.dji.flydji_new;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.flightassistant.PerceptionInformation;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.internal.camera.W;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "MapActivity";

    private GoogleMap gMap;
    private Button add, clear, locate, mConfig, mUpload, mStart, mStop, mAction;
    private TextView mLat;
    private TextView mLong;
    private TextView mSatCount;
    private TextView mDistance;
    private TextView mFlightTime;
    private TextView mWaypointCount;
    private TextView mVelocity;
    private TextView mSignal;
    private GPSSignalLevel mLevel;
    private Spinner s;
    private SwitchCompat mSatellite;
    private Marker droneMarker = null;
    private List<LatLng> latLngList = new ArrayList<>();
    private Polyline polyline = null;
    private float sum = 0;
    private FlightController flightController;
    private double droneLocationLat, droneLocationLng;
    private double droneVelX, droneVelY, droneVelZ, Vel;
    private int droneSatellite;
    private boolean isAdd = false;
    private FusedLocationProviderClient fusedLocationClient;


    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private List<Waypoint> waypointList = new ArrayList<>();
    private float altitude, mSpeed;
    private int waypointNO;

    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private WaypointActionType mPhoto, mVideoRec, mVideoStop, mStay, mPitch, mRotate;

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
        removeListener();
    }

    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CHANGE_WIFI_STATE, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }
        setContentView(R.layout.activity_map);

        //Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FlyDJI.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        addListener();
    }

    public void initUI() {

        // Initialize TextViews
        mLat = findViewById(R.id.txt_lat);
        mLong = findViewById(R.id.txt_long);;
        mSatellite = findViewById(R.id.sw_satellite);
        mSatCount = findViewById(R.id.txt_satcount);
        mDistance = findViewById(R.id.distance);
        mFlightTime = findViewById(R.id.flightTime);
        mWaypointCount = findViewById(R.id.waypointCount);
        mVelocity = findViewById(R.id.velocity);
        mSignal = findViewById(R.id.gpsLevel);


        // Initialize buttons
        mConfig = findViewById(R.id.config);
        mUpload = findViewById(R.id.upload);
        mStart = findViewById(R.id.start);
        mStop = findViewById(R.id.stop);
        locate = findViewById(R.id.locate);
        add = findViewById(R.id.add);
        clear = findViewById(R.id.clear);
        mAction = findViewById(R.id.addAction);

        mConfig.setOnClickListener(this);
        mUpload.setOnClickListener(this);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        mAction.setOnClickListener(this);
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

    private void initFlightController() {

        BaseProduct product = FlyDJI.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }

        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    droneLocationLat = flightControllerState.getAircraftLocation().getLatitude();
                    droneLocationLng = flightControllerState.getAircraftLocation().getLongitude();
                    droneSatellite = flightControllerState.getSatelliteCount();
                    droneVelX = flightControllerState.getVelocityX();
                    droneVelY = flightControllerState.getVelocityY();
                    droneVelZ = flightControllerState.getVelocityZ();
                    mLevel = flightControllerState.getGPSSignalLevel();
                    Vel = Math.sqrt(droneVelX*droneVelX + droneVelY*droneVelY * droneVelZ*droneVelZ);
                    int flightTime = flightControllerState.getFlightTimeInSeconds();
                    int minutes = (flightTime % 3600) / 60;
                    int seconds = flightTime % 60;
                    final String timeString = String.format("%02d:%02d", minutes, seconds);
                    final boolean isFlying = flightControllerState.isFlying();
                    MapActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFlying)
                            {
                                mFlightTime.setText("Flight Time: " + timeString);
                            }
                            else
                            {
                                mFlightTime.setText("Flight Tme: ");
                            }
                        }
                    });
                    updateDroneLocation();
                }
            });
        }

    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateDroneLocation() {

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                    mLat.setText("Latitude: " + String.format("%.5f", droneLocationLat));
                    mLong.setText("Longitude: " + String.format("%.5f", droneLocationLng));
                    mSatCount.setText("No. of satellite: " + droneSatellite);
                    mVelocity.setText("Velocity: " + String.format("%.3f", Vel) + " m/s");
                    mSignal.setText("GPS SIGNAL: " + mLevel);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {

            switch (v.getId()) {

                case R.id.locate: {
                    updateDroneLocation();
                    cameraUpdate(); // Locate the drone's place
                    break;
                }
                case R.id.add: {
                    enableDisableAdd();
                    break;
                }
                case R.id.clear: {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gMap.clear();
                        }

                    });
                    sum = 0;
                    mDistance.setText("Distance: ");
                    mWaypointCount.setText("Waypoints: ");
                    latLngList.clear();
                    waypointList.clear();
                    mFlightTime.setText("Flight Time: ");
                    waypointMissionBuilder.waypointList(waypointList);
                    updateDroneLocation();
                    break;
                }
                case R.id.config: {
                    showSettingDialog();
                    break;
                }
                case R.id.upload: {
                    uploadWayPointMission();
                    break;
                }
                case R.id.start: {
                    startWaypointMission();
                    break;
                }
                case R.id.stop: {
                    stopWaypointMission();
                    break;
                }
                case R.id.addAction: {
                    showActionSettingDialog();
                    break;
                }
                default:
                    break;

        }
    }

    private void enableDisableAdd() {
        if (!isAdd) {
            isAdd = true;
            add.setText("Exit");
        } else {
            isAdd = false;
            add.setText("Add");
        }
    }

    private void cameraUpdate() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);
    }

    private void showActionSettingDialog()
    {
        LinearLayout actionSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointactionsetting, null);
        RadioGroup action_RG1 = actionSettings.findViewById(R.id.actions);
        RadioGroup action_RG2 = actionSettings.findViewById(R.id.actions2);
        ArrayAdapter<Waypoint> waypointArrayAdapter = new ArrayAdapter<Waypoint>(this, android.R.layout.simple_spinner_item, waypointList);
        waypointArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s = (Spinner) actionSettings.findViewById(R.id.waypointNumber);
        s.setAdapter(waypointArrayAdapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                waypointNO = parent.getSelectedItemPosition();
                setResultToToast("Waypoint " + (waypointNO + 1)  + " selected");

                action_RG1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.photoMode)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
                        }
                        else if (checkedId == R.id.videoModeStart)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.START_RECORD, 0));
                        }
                        else if (checkedId == R.id.videoModeStop)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.STOP_RECORD, 0));
                        }
                        else if (checkedId == R.id.removeAction)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO).removeActionAtIndex(waypointNO);
                        }
                    }
                });
                action_RG2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.stay)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.STAY, 10000));
                        }
                        else if (checkedId == R.id.gimbalPitch)
                        {
                            waypointMissionBuilder.setGimbalPitchRotationEnabled(true);
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
                        }
                        else if (checkedId == R.id.aircraftRotation)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO)
                                    .addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 45));
                        }
                        else if (checkedId == R.id.removeAction2)
                        {
                            waypointMissionBuilder.getWaypointList().get(waypointNO).removeActionAtIndex(waypointNO);
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setResultToToast("No item selected!");
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(actionSettings)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        setResultToToast("Waypoint " + (waypointNO + 1) + " action added/removed!");
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    private void showSettingDialog() {
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed) {
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone) {
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding) {
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                    flightController.setStateCallback(new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState flightControllerState) {
                            if(flightControllerState.isLandingConfirmationNeeded())
                            {
                                flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        setResultToToast("Landing confirmed");
                                    }
                                });
                            }
                        }
                    });
                } else if (checkedId == R.id.finishToFirst) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefault(altitudeString));
                        Log.e(TAG, "altitude " + altitude);
                        Log.e(TAG, "speed " + mSpeed);
                        Log.e(TAG, "mFinishedAction " + mFinishedAction);
                        Log.e(TAG, "mHeadingMode " + mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefault(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        // TODO Auto-generated method stub
        // Initializing gMap object
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null)
                {
                    LatLng CURRENT = new LatLng(location.getLatitude(), location.getLongitude());
                    gMap.addMarker(new MarkerOptions().position(CURRENT).title("Current position"));
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CURRENT, 18));
                }
            }
        });

        if(checkGpsCoordination(droneLocationLat, droneLocationLng))
        {
            latLngList.add(0, new LatLng(droneLocationLat, droneLocationLng));
            flightController.setHomeLocationUsingAircraftCurrentLocation(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    setResultToToast("Home location set to\nLAT: " +droneLocationLat + "\nLNG: " + droneLocationLng);
                }
            });
        }
        mSatellite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mSatellite.isChecked())
                {
                    gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for gMap object
    }


    private void setResultToToast(final String string){
        MapActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        markerOptions.position(point).title("Waypoint " + (waypointList.size() + 1));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        if(flightController != null && altitude >= 20)
        {
            flightController.setGoHomeHeightInMeters((int) altitude, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null)
                    {
                        setResultToToast("Go Home height set to " + altitude + "m");
                    }
                }
            });
        }
        else
        {
            flightController.setGoHomeHeightInMeters(20, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    setResultToToast("Go Home height set to 20m");
                }
            });
        }


        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
            setCollisionAvoidanceDisabled();
        }
    };

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                latLngList.add(point);
                mWaypointCount.setText("Waypoints: " + waypointList.size());
                PolylineOptions polylineOptions = new PolylineOptions().addAll(latLngList).clickable(true);
                polyline = gMap.addPolyline(polylineOptions);
                float[] results = new float[1];
                for (int i = 0; i < latLngList.size() - 1; i++)
                {
                    Location.distanceBetween(
                            latLngList.get(i).latitude,
                            latLngList.get(i).longitude,
                            latLngList.get(i+1).latitude,
                            latLngList.get(i+1).longitude,
                            results
                    );
                }

                sum += results[0];
                if(sum >= 1000)
                {
                    mDistance.setText("Distance: " + String.format("%.3f", sum/1000) + " km");
                }
                else
                {
                    mDistance.setText("Distance: " + String.format("%.3f", sum) + " m");
                }
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                latLngList.add(point);
                PolylineOptions polylineOptions = new PolylineOptions().addAll(latLngList).clickable(true);
                polyline = gMap.addPolyline(polylineOptions);
                float[] results = new float[1];
                for (int i = 0; i < latLngList.size() - 1; i++)
                {
                    Location.distanceBetween(
                            latLngList.get(i).latitude,
                            latLngList.get(i).longitude,
                            latLngList.get(i+1).latitude,
                            latLngList.get(i+1).longitude,
                            results
                    );
                }
                sum += results[0];
                if(sum >= 1000)
                {
                    mDistance.setText("Distance: " + String.format("%.3f", sum/1000) + " km");
                }
                else
                {
                    mDistance.setText("Distance: " + String.format("%.3f", sum) + " m");
                }
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        setCollisionAvoidanceEnabled();
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void setCollisionAvoidanceEnabled()
    {
        flightController.getFlightAssistant().setCollisionAvoidanceEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                setResultToToast("Collision Avoidance enabled");
            }
        });

        flightController.setTerrainFollowModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
    }

    private void setCollisionAvoidanceDisabled()
    {
        flightController.getFlightAssistant().setCollisionAvoidanceEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                setResultToToast("Collision Avoidance disabled");
            }
        });

        flightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
    }
}