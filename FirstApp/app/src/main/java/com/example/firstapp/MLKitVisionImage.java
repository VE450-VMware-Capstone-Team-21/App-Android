package com.example.firstapp;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;

public class MLKitVisionImage {
    private void imageFromBitmap(Bitmap bitmap) {
        int rotationDegree = 0;
        // [START image_from_bitmap]
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
        // [END image_from_bitmap]
    }

}
