/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.augmentedimage.flowgate.flowgateClient;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.mlkit.vision.common.InputImage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * ArAugmentedImage_getTrackingMethod() and render only when the tracking method equals to
 * AR_AUGMENTED_IMAGE_TRACKING_METHOD_FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/c/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity implements AugmentedImageFragment.OnCompleteListener {
  private ArFragment arFragment;
  private ImageView fitToScanView;
  private Button button;
  int imgCnt = 0;
  AugmentedImageNode deviceNode;

  private TextView serverTextview;
  private TextView rackTextview;
  private ViewRenderable serverRenderable;
  private ViewRenderable rackRenderable;
  private AnchorNode serverNode;
  private AnchorNode rackNode;

  private Boolean isScanSuccess = false;

  flowgateClient fc = new flowgateClient("202.121.180.32", "admin", "Ar_InDataCenter_450");

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Create new fragment and transaction
    arFragment = new AugmentedImageFragment();
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

    // Replace whatever is in the fragment_container view with this fragment,
    // and add the transaction to the back stack
    transaction.replace(R.id.ux_fragment, arFragment);
    transaction.addToBackStack(null);
    // Commit the transaction
    transaction.commit();

    // arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    button = findViewById(R.id.button1);
    // Reset the app status.
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isScanSuccess = false;
        serverNode = null;
        imgCnt = 0;
        if (serverTextview != null){
          serverTextview.setText("");
        }
        if (rackTextview != null){
          rackTextview.setText("");
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.detach(arFragment);
        transaction.attach(arFragment);
        transaction.commit();
      }
    });
  }

  @Override
  public void onComplete() {
    SnackbarHelper.getInstance().showMessage(this, "Detecting");

    // Build the 2D renderable for text views of server & rack.
    ViewRenderable.builder()
        .setView(this, R.layout.server_view)
        .build()
        .thenAccept(renderable -> serverRenderable = renderable);
    ViewRenderable.builder()
        .setView(this, R.layout.rack_view)
        .build()
        .thenAccept(renderable -> rackRenderable = renderable);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      //fitToScanView.setVisibility(View.VISIBLE);
      fitToScanView.setVisibility(View.GONE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();
    Session session = arFragment.getArSceneView().getSession();

    if (session == null || frame == null) {
      return;
    }

    // Let the fragment update its state first.
    arFragment.onUpdate(frameTime);

    // If ARCore is not tracking yet, then don't process anything.
    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    serverTextview = (TextView) serverRenderable.getView();
    rackTextview = (TextView) rackRenderable.getView();

    // Scan once; and once the information is obtained, no scanning before reset.
    if (!isScanSuccess) {
      /*if (this.serverNode != null) {
        serverNode.getAnchor().detach();
        serverNode.setParent(null);
        serverNode.setRenderable(null);
        serverNode = null;
        augmentedImageMap.forEach((image, node) -> {
          arFragment.getArSceneView().getScene().removeChild(node);
          node = null;
        });
        augmentedImageMap.clear();
      }*/
      // Send frame image and scan barcode.
      try (final Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage()) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
          Bitmap bitmapImage = YUV420toBitmap.getBitmap(image);
          BarcodeScan barScanning = new BarcodeScan();
          InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);

          Context context = this.getApplicationContext();

          barScanning.scanBarcodes(inputImage, fc, context, serverTextview, this);
          image.close();

          // Get rack data.
          Thread tRack = new Thread(){
            @Override
            public void run(){
              fc.getAssetByNameOnScreen(context, "Cabinet0001", rackTextview);
            }
          };
          tRack.start();
          try{
            tRack.join();
          }
          catch (Exception e){
            e.printStackTrace();
          }
        }
      } catch (NotYetAvailableException e) {
        Log.e("Barcode", "NotYetAvailableException sending frame image.", e);
      }
    }

    isScanSuccess = (serverTextview.getText().toString().length() != 0);

    // Image recognition after barcode scanning.
    if (isScanSuccess) {
      Collection<AugmentedImage> updatedAugmentedImages =
          frame.getUpdatedTrackables(AugmentedImage.class);
      for (AugmentedImage augmentedImage : updatedAugmentedImages) {
        switch (augmentedImage.getTrackingState()) {
          case PAUSED:
            // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
            // but not yet tracked.
            String text = "Detected Image " + augmentedImage.getIndex();
            SnackbarHelper.getInstance().showMessage(this, text);
            break;

          case TRACKING:
            // Have to switch to UI Thread to update View.
            fitToScanView.setVisibility(View.GONE);

            // Create a new anchor for newly found images.
            if (!augmentedImageMap.containsKey(augmentedImage)) {
              SnackbarHelper.getInstance().showMessage(this, "Device detected and frame drawn");
              if (imgCnt == 0) {
                this.deviceNode = new AugmentedImageNode(imgCnt);
                this.deviceNode.setImage(augmentedImage, this);
                augmentedImageMap.put(augmentedImage, this.deviceNode);
                imgCnt = 1;
              }
              else if(imgCnt == 1){
                AugmentedImageNode node = new AugmentedImageNode(imgCnt);
                node.setImage(augmentedImage, this);
                augmentedImageMap.put(augmentedImage, node);

                arFragment.getArSceneView().getScene().addChild(node);
                arFragment.getArSceneView().getScene().addChild(this.deviceNode);
                imgCnt = 0;
              }

              // Put the server's information near the cabinet.
              if (this.serverNode == null && imgCnt == 0) {
                float[] pos = {augmentedImage.getCenterPose().tx() + 1.5f*augmentedImage.getExtentX(),
                    augmentedImage.getCenterPose().ty(), augmentedImage.getCenterPose().tz()};
                float[] rotation = {0, 1, 0, 0};
                Anchor anchor = session.createAnchor(new Pose(pos, rotation));
                serverNode = new AnchorNode(anchor);
                serverNode.setRenderable(serverRenderable);
                serverNode.setParent(arFragment.getArSceneView().getScene());

                float[] pos2 = {augmentedImage.getCenterPose().tx() - 1.5f*augmentedImage.getExtentX(),
                    augmentedImage.getCenterPose().ty(), augmentedImage.getCenterPose().tz()};
                Anchor anchor2 = session.createAnchor(new Pose(pos2, rotation));
                rackNode = new AnchorNode(anchor2);
                rackNode.setRenderable(rackRenderable);
                rackNode.setParent(arFragment.getArSceneView().getScene());
              }
            }
            break;

          case STOPPED:
            augmentedImageMap.remove(augmentedImage);
            break;
        }
      }
    }
  }
}
