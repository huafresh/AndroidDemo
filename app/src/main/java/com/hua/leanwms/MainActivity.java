package com.hua.leanwms;


import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.hua.camerademo.CameraActivity;
import com.hua.leanwms.permission.RxPermissions;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
         findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 new RxPermissions(MainActivity.this)
                         .request(Manifest.permission.CAMERA)
                         .subscribe(new Consumer<Boolean>() {
                             @Override
                             public void accept(Boolean aBoolean) throws Exception {
                                 startActivity(new Intent(MainActivity.this, CameraActivity.class));

                             }
                         });
             }
         });
    }
}
