// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.intel.ngs.aioffload.facedetection;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.intel.ngs.aioffload.common.CameraImageGraphic;
import com.intel.ngs.aioffload.common.FrameMetadata;
import com.intel.ngs.aioffload.common.GraphicOverlay;
import com.intel.ngs.aioffload.VisionProcessorBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceDetectionProcessor";
    private static final String HOST = "";
    private static final int    PORT = 0;

    private final FirebaseVisionFaceDetector detector;
    private final GrpcClient grpcClient = null;

    public FaceDetectionProcessor() {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        //grpcClient = new GrpcClient(HOST, PORT);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap camBitmap,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();

        //[KBK:KBK] Add zoom here to make front cam full screen?
        if (camBitmap != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, camBitmap);
            graphicOverlay.add(imageGraphic);
        }

        ArrayList<Bitmap> croppedBmps = new ArrayList<Bitmap>();

        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);

            if (camBitmap != null) {
                int cropX = (camBitmap.getWidth() - face.getBoundingBox().centerX()) - (face.getBoundingBox().width()/2) < 0 ?
                    0: (camBitmap.getWidth() - face.getBoundingBox().centerX()) - (face.getBoundingBox().width()/2);
                int cropY = face.getBoundingBox().centerY() - (face.getBoundingBox().height()/2) < 0 ?
                    0: face.getBoundingBox().centerY() - (face.getBoundingBox().height()/2);
                int cropWidth = cropX + face.getBoundingBox().width() > camBitmap.getWidth()-1 ?
                    camBitmap.getWidth() - cropX: face.getBoundingBox().width();
                int cropHeight = cropY + face.getBoundingBox().height() > camBitmap.getHeight()-1 ?
                    camBitmap.getHeight() - cropY: face.getBoundingBox().height();

                croppedBmps.add(Bitmap.createBitmap(camBitmap, cropX, cropY, cropWidth, cropHeight));
            }

            Bitmap firstCroppedImage = null;
            if (croppedBmps.size() != 0) {
                firstCroppedImage = croppedBmps.get(0);
            }
            int cameraFacing =
                frameMetadata != null ? frameMetadata.getCameraFacing() :
                    Camera.CameraInfo.CAMERA_FACING_BACK;
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, cameraFacing, firstCroppedImage);
            graphicOverlay.add(faceGraphic);

        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
