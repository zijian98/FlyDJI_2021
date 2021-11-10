package com.dji.flydji_new;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CardView mCamera, mFlightControl, mMap, mPlayback, mPoint, mTrack;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCamera = findViewById(R.id.camera_card);
        mFlightControl = findViewById(R.id.flight_card);
        mMap = findViewById(R.id.map_card);
        mPlayback = findViewById(R.id.playback_card);
        mPoint = findViewById(R.id.point_card);
        mTrack = findViewById(R.id.track_card);

        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openCamera(); }
        });

        mFlightControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openFlightControl(); }
        });

        mMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openMap(); }
        });

        mPlayback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openPlayback(); }
        });

        mPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openPoint(); }
        });

        mTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openTrack(); }
        });
    }

    private void openCamera()
    {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void openFlightControl()
    {
        Intent intent = new Intent(this, FlightActivity.class);
        startActivity(intent);
    }

    private void openMap()
    {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void openPlayback()
    {
        Intent intent = new Intent(this, PlaybackActivity.class);
        startActivity(intent);
    }

    private void openPoint()
    {
        Intent intent = new Intent(this, PointingActivity.class);
        startActivity(intent);
    }

    private void openTrack()
    {
        Intent intent = new Intent(this, TrackingActivity.class);
        startActivity(intent);
    }


}
