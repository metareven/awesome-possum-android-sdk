package com.telenor.possumcore.detectors;

import android.Manifest;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.facedetection.FaceDetector;
import com.telenor.possumcore.facedetection.FaceProcessor;
import com.telenor.possumcore.facedetection.FaceTracker;
import com.telenor.possumcore.facedetection.IFaceFound;
import com.telenor.possumcore.neuralnetworks.TensorWeights;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.opencv.imgproc.Imgproc.getAffineTransform;
import static org.opencv.imgproc.Imgproc.warpAffine;

public class ImageDetector extends AbstractDetector implements IFaceFound {
    private static TensorWeights tensorFlowInterface;
    private static FaceDetector detector; // To prevent changes during configChanges it is static
    private CameraSource cameraSource;
    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private static final int OUTPUT_BMP_WIDTH = 96;
    private static final int OUTPUT_BMP_HEIGHT = 96;
    private boolean isProcessingFace;
    private static final String modelName = "tensorflow_facerecognition.pb";

    public ImageDetector(@NonNull Context context) {
        super(context);
        setupCameraSource();
        try {
            tensorFlowInterface = createTensor(context.getAssets(), modelName);
            OpenCVLoader.initDebug(context());
        } catch (Exception e) {
            Log.e(tag, "AP: Failed to initialize tensorFlow or openCV:",e);
        }
    }

    /**
     * The face detector is the actual detector finding the face in a video stream. It handles
     * setting up google vision, setting the custom face detector to report faces in the interface
     * and the processor handling it.
     * <p>
     * TODO: WARNING: It will require special treatment for configuration changes (viewport rotation)
     */
    private void setupFaceDetector() {
        if (detector == null || detector.isReleased()) {
            com.google.android.gms.vision.face.FaceDetector.Builder visionBuilder = new com.google.android.gms.vision.face.FaceDetector.Builder(context());
            visionBuilder.setLandmarkType(com.google.android.gms.vision.face.FaceDetector.ALL_LANDMARKS);
            visionBuilder.setTrackingEnabled(false);
            visionBuilder.setProminentFaceOnly(true);
            visionBuilder.setMode(com.google.android.gms.vision.face.FaceDetector.FAST_MODE);
            com.google.android.gms.vision.face.FaceDetector googleFaceDetector = visionBuilder.build();
            detector = new FaceDetector(googleFaceDetector, this);
            detector.setProcessor(new FaceProcessor(detector, new FaceTracker()));
        } else {
            Log.i(tag, "Attempt to recreate but it was already there...");
        }
    }

    /**
     * Sets up the camera source (and the face detector before it). Note that this process is time
     * consuming and should be done as early as possible (taking about 2 seconds to complete after
     * it starts). This precludes setting it up for each run()/scan as it would take most of the
     * time required to identity faces just to set it up.
     */
    private void setupCameraSource() {
        setupFaceDetector();
        cameraSource = new CameraSource.Builder(context(), detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .setRequestedFps(30)
                .build();
    }

    @Override
    public int detectorType() {
        return DetectorType.Image;
    }

    @Override
    public String detectorName() {
        return "image";
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void run() {
        if (isEnabled() && isAvailable()) {
            isProcessingFace = false;
            Log.i(tag, "FirstTest: Starting camera for image");
            try {
                cameraSource.start();
            } catch (IOException e) {
                Log.i(tag, "IO:", e);
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void terminate() {
        if (isPermitted()) {
            cameraSource.stop();
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && tensorFlowInterface != null;
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable();
    }

    @Override
    public void faceFound(Face face, Frame frame) {
        if (face == null || isProcessingFace) return;
        PointF leftEye = null;
        PointF rightEye = null;
        PointF mouth = null;
        // TODO: IMPORTANT: Some devices have differing output from CameraSource. Some output landscape, other portrait. How to handle this?
        int orientation = -1;
        switch (frame.getMetadata().getRotation()) {
            case 1:
                orientation = 0;
                break;
            case 3:
                orientation = -90;//180;
                break;
            case 6:
                orientation = 0;//270;
                break;
            case 8:
                orientation = 0;//360;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        byte[] imgBytes = getBytesFromFrame(frame);
        Bitmap imageBeforeProcess = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
        final Bitmap imageProcessed;
        if (orientation != 0)
            imageProcessed = Bitmap.createBitmap(imageBeforeProcess, 0, 0, imageBeforeProcess.getWidth(), imageBeforeProcess.getHeight(), matrix, false);
        else imageProcessed = imageBeforeProcess;

        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == Landmark.LEFT_EYE) leftEye = landmark.getPosition();
            else if (landmark.getType() == Landmark.RIGHT_EYE) rightEye = landmark.getPosition();
            else if (landmark.getType() == Landmark.BOTTOM_MOUTH) mouth = landmark.getPosition();
        }
        if (leftEye != null && rightEye != null && mouth != null) {
            isProcessingFace = true;
            Log.d(tag, "AP: Face found");
            PointF centroid = new PointF((rightEye.x + leftEye.x + mouth.x) / 3, (rightEye.y + leftEye.y + mouth.y) / 3);
            float diffX = 1.5f * Math.abs(leftEye.x - rightEye.x);
            RectF faceFrame = new RectF(centroid.x - diffX, centroid.y - diffX, centroid.x + diffX, centroid.y + diffX);
            if (faceFrame.left < 0 || faceFrame.top < 0) {
                // Unable to get a frame around the necessary area
                isProcessingFace = false;
                return;
            }
            Bitmap fixedImage = Bitmap.createBitmap(imageProcessed, (int) faceFrame.left, (int) faceFrame.top, (int) faceFrame.width(), (int) faceFrame.height());
            PointF movedLeftEye = new PointF(leftEye.x - faceFrame.left, leftEye.y - faceFrame.top);
            PointF movedRightEye = new PointF(rightEye.x - faceFrame.left, rightEye.y - faceFrame.top);
            PointF movedMouth = new PointF(mouth.x - faceFrame.left, mouth.y - faceFrame.top);

            Log.d(tag, "AP: Before aligning face");
            Bitmap alignedFace = alignFace(fixedImage, movedLeftEye, movedRightEye, movedMouth);
            if (alignedFace == null) {
                Log.d(tag, "AP: Aligned face");
                isProcessingFace = false;
                return;
            }

            final Bitmap scaledOutput = Bitmap.createScaledBitmap(alignedFace, OUTPUT_BMP_WIDTH, OUTPUT_BMP_HEIGHT, false);
            float[] rgbArray = bitmapToIntArray(scaledOutput);
            streamData(tensorFlowInterface.getWeights(rgbArray, now()));
            isProcessingFace = false;
        }
    }

    /**
     * Method for converting a frame from google's vision api to a byteArray
     *
     * @param frame a google vision frame
     * @return a byte array of the image
     */
    private byte[] getBytesFromFrame(Frame frame) {
        int height = frame.getMetadata().getHeight();
        int width = frame.getMetadata().getWidth();
        YuvImage yuvimage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream); // Where 100 is the quality of the generated jpeg
        return byteArrayOutputStream.toByteArray();
    }

    private static float[] bitmapToIntArray(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);
        if (width != height) {
            throw new java.lang.Error("BitmapToIntArray only makes sense on square images");
        }
        float[] intArray = new float[width * width * 3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                intArray[i * width + 3 * j] = (pixels[i * j] >> 16) & 0xff;
                intArray[i * width + 3 * j + 1] = (pixels[i * j] >> 8) & 0xff;
                intArray[i * width + 3 * j + 2] = pixels[i * j] & 0xff;
            }
        }
        return intArray;
    }

/*    public static Matrix affineTransform(float width, float height, PointF leftEye, PointF rightEye, PointF mouth) {
        Matrix src = new Matrix();
        Matrix dest = new Matrix();

        // our reference points (source)
        src.mapPoints(new float[]{leftEye.x, leftEye.y, rightEye.x, rightEye.y, mouth.x, mouth.y});
        // http://openface-api.readthedocs.io/en/latest/openface.html
        // Alex: calculated from python script where inner eyes are interpolated from four eye points (also norm min/max)
        dest.mapPoints(new float[]{ (float) (width * 0.70726717), (float) (height * 0.1557629),
                (float) (width * 0.27657071), (float) (height * 0.16412275),
                (float) (width * 0.50020397), (float) (height * 0.75058442)});
        Matrix matrixTransformation = new Matrix();

        return matrixTransformation;
    }*/

    protected TensorWeights createTensor(AssetManager assetManager, String modelName) {
        return new TensorWeights(assetManager, modelName);
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        if (detector != null) {
            detector.destroy();
        }
    }

    private static Bitmap alignFace(Bitmap face, PointF leftEye, PointF rightEye, PointF mouth) {
        if (face == null) {
            Log.i(tag, "AP: Bitmap is null w00t!");
            return null;
        }
//        Matrix affineTransform = affineTransform(face.getWidth(), face.getHeight(), leftEye, rightEye, mouth);



       // float[] pixels = bitmapToIntArray(face);
        MatOfPoint2f src = new MatOfPoint2f();
        MatOfPoint2f dest = new MatOfPoint2f();

        // our reference points (source)
        src.fromArray(new Point(leftEye.x, leftEye.y),
                new Point(rightEye.x, rightEye.y),
                new Point(mouth.x, mouth.y));

        double dimX = face.getWidth();
        double dimY = face.getHeight();
        // http://openface-api.readthedocs.io/en/latest/openface.html
        // Alex: calculated from python script where inner eyes are interpolated from four eye points (also norm min/max)
        dest.fromArray(new Point(dimX * 0.70726717, dimY * 0.1557629),
                new Point(dimX * 0.27657071, dimY * 0.16412275),
                new Point(dimX * 0.50020397, dimY * 0.75058442));

        Mat matrixTransformation = getAffineTransform(src, dest);
        Mat orgImage = new Mat();
        Mat alignedImage = new Mat();

        Utils.bitmapToMat(face, orgImage);

        warpAffine(orgImage, alignedImage, matrixTransformation, orgImage.size());

        Bitmap alignedFace = Bitmap.createBitmap(face.getWidth(), face.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(alignedImage, alignedFace);

        return alignedFace;
    }
//
//    public void setModel(TensorFlowInferenceInterface model) {
//        tensorFlowInterface = model;
//        Log.i(tag, "FirstTest: tensorFlowInterface assigned");
//    }

/*    protected int[] LBPFromBitmap(Bitmap bitmap) {
        int[] vectorLBPU = new int[0];
        try {
            // Convert the input image to a matrix image
            double[][] realImage = imageConversion(image);

            // Calculate the number of squares
            int bw = 8;
            int bh = 8;
            int nbx = (int) Math.floor(realImage.length / bw);
            int nby = (int) Math.floor(realImage.length / bh);

            // Create the LBP vector
            vectorLBPU = exec(realImage, nbx, nby, bw, bh);

        }
    }*/

    @Override
    public String requiredPermission() {
        return Manifest.permission.CAMERA;
    }
}