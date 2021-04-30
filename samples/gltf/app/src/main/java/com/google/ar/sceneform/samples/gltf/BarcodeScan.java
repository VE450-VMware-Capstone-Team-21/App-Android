package com.google.ar.sceneform.samples.gltf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

import com.google.ar.sceneform.samples.gltf.flowgate.flowgateClient;

public class BarcodeScan {
	private static final String TAG = "Barcode Detect";
	public void scanBarcodes(InputImage image, flowgateClient fc, Context context, TextView textView) {
		BarcodeScannerOptions options =
			new BarcodeScannerOptions.Builder()
				.setBarcodeFormats(Barcode.FORMAT_CODE_128)
				.build();

		BarcodeScanner scanner = BarcodeScanning.getClient();
		// Or, to specify the formats to recognize:
		Log.d(TAG, "set options");
//        BarcodeScanner scanner = BarcodeScanning.getClient(options);

		Log.d(TAG, "getClient");
		Task<List<Barcode>> result = scanner.process(image)
			.addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
				@Override
				public void onSuccess(List<Barcode> barcodes) {
					// Task completed successfully
					// [START_EXCLUDE]
					// [START get_barcodes]
					for (Barcode barcode: barcodes) {
//
						String rawValue = barcode.getRawValue();
						Log.d(TAG, "The id is: " + rawValue);

						Thread t = new Thread(){
							@Override
							public void run(){
								fc.getAssetByIdOnScreen(context, rawValue, textView);
							}
						};
						t.start();
						try{
							t.join();
						}
						catch (Exception e){
							e.printStackTrace();
						}
					}
				}
			})
			.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.d(TAG, "Failure");
				}
			});
	}
}
