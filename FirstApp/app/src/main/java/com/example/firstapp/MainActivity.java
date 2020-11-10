package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.mlkit.vision.common.InputImage;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.firstapp.MESSAGE";
//    public static String a;
    public static Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }
    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void scanBarcode(View view) {
        String TAG = "scanBarcode";
        // Do something in response to button

//        Intent intent = new Intent( this, BarcodeScanningActivity.class);
        bitmap = BitmapFactory.decodeResource(this.getApplicationContext().getResources(), R.drawable.test);
        BarcodeScan barScanning = new BarcodeScan();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Log.d(TAG, "before scan");
        barScanning.scanBarcodes(image);
        Log.d(TAG, "after scan");

//        EditText editText = (EditText) findViewById(R.id.editText);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent);
    }


}