package com.hua.camerademo;

import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author zhangsh
 * @version V1.0
 * @date 2020-02-19 17:06
 */

public class CameraActivity extends AppCompatActivity {

    private OrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ImageView result = findViewById(R.id.image);
        result.setImageResource(R.drawable.test);
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                e.printStackTrace();
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                e.printStackTrace();
            }
        });

        CameraDisplayView cameraDisplayView = findViewById(R.id.camera_display);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraDisplayView.takePicture(new CameraDisplayView.PictureCallback() {
                    @Override
                    public void onPictureTaken(Bitmap bitmap) {
                        result.setImageBitmap(bitmap);
                    }
                });
            }
        });

        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraDisplayView.switchCamera();
            }
        });

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                cameraDisplayView.onOrientationChanged(orientation);
            }
        };
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            orientationEventListener.disable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        orientationEventListener.disable();
    }
}
