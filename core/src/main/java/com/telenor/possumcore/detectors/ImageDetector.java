package com.telenor.possumcore.detectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.facedetection.FaceDetector;
import com.telenor.possumcore.facedetection.FaceProcessor;
import com.telenor.possumcore.facedetection.FaceTracker;
import com.telenor.possumcore.facedetection.IFaceFound;
import com.telenor.possumcore.interfaces.IDetectorChange;
import com.telenor.possumcore.neuralnetworks.TensorWeights;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import static org.opencv.imgproc.Imgproc.getAffineTransform;
import static org.opencv.imgproc.Imgproc.warpAffine;

/**
 * Uses your back camera to try to get a facial assessment, utilizing image recognition to see
 * whether you are yourself or not
 */
public class ImageDetector extends AbstractDetector implements IFaceFound {
    private static TensorWeights tensorFlowInterface;
    private static FaceDetector detector; // To prevent changes during configChanges it is static
    private CameraSource cameraSource;
    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private static final int OUTPUT_BMP_WIDTH = 96;
    private static final int OUTPUT_BMP_HEIGHT = 96;
    private boolean isProcessingFace;
    private JsonParser parser;
    private Gson gson;
    private static final String lbpDataSet = "image_lbp";
    private static final String modelName = "tensorflow_facerecognition.pb";

    public ImageDetector(@NonNull Context context) {
        this(context, null);
    }
    public ImageDetector(@NonNull Context context, IDetectorChange listener) {
        super(context, listener);
        parser = new JsonParser();
        gson = new Gson();
        setupCameraSource();
        createDataSet(lbpDataSet);
        try {
            tensorFlowInterface = createTensor(context.getAssets(), modelName);
            initializeOpenCV();
        } catch (Exception e) {
            Log.e(tag, "AP: Failed to initialize tensorFlow or openCV:", e);
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
     * Separate method to handle openCV in order to ease testing
     */
    protected void initializeOpenCV() {
        OpenCVLoader.initDebug(context());
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
    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public void run() {
        super.run();
        if (isEnabled() && isAvailable()) {
            isProcessingFace = false;
            try {
                cameraSource.start();
            } catch (IOException e) {
                Log.i(tag, "AP: IO:", e);
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
        return tensorFlowInterface != null && context().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
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
            case Frame.ROTATION_0://1:
                orientation = 0;
                break;
            case Frame.ROTATION_90://3:
                orientation = -90;//180;
                break;
            case Frame.ROTATION_180://6:
                orientation = -180;//270;
                break;
            case Frame.ROTATION_270://8:
                orientation = 90;//360;
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

            Bitmap alignedFace = alignFace(fixedImage, movedLeftEye, movedRightEye, movedMouth);
            if (alignedFace == null) {
                isProcessingFace = false;
                return;
            }

            final Bitmap scaledOutput = Bitmap.createScaledBitmap(alignedFace, OUTPUT_BMP_WIDTH, OUTPUT_BMP_HEIGHT, false);

            long nowTimestamp = now();

            // Tensor weights
            streamData(tensorFlowInterface.getWeights(scaledOutput, nowTimestamp));

            // LBP array
            JsonArray lbpArray = new JsonArray();
            lbpArray.add(""+nowTimestamp);
            lbpArray.add(parser.parse(gson.toJson(mainLBP(scaledOutput))));
            lbpArray.add(landMarks(face));
            streamData(lbpArray, lbpDataSet);

            isProcessingFace = false;
        }
    }

    int[] mainLBP(Bitmap image) {
        // Convert the input image to a matrix image
        double[][] realImage = imageConversion(image);

        // Calculate the number of squares
        int bw = 8; // Magic number
        int bh = 8; // Magic number
        int nbx = (int) Math.floor(realImage.length / bw);
        int nby = (int) Math.floor(realImage.length / bh);
        // Create the LBP vector
        return LBPVector(realImage, nbx, nby, bw, bh);
    }

    public JsonArray landMarks(Face face) {
        JsonArray landmarks = new JsonArray();
        for (Landmark landmark : face.getLandmarks()) {
            JsonArray landmarkSet = new JsonArray();
            landmarkSet.add(""+ landmark.getType());
            landmarkSet.add(""+landmark.getPosition().x);
            landmarkSet.add(""+landmark.getPosition().y);
            landmarks.add(landmarkSet);
        }
        return landmarks;
    }

    // Function to convert a image to a double matrix image
    private double[][] imageConversion(Bitmap image) {
        if (image.getWidth() != OUTPUT_BMP_HEIGHT || image.getHeight() != OUTPUT_BMP_HEIGHT)
            image = Bitmap.createScaledBitmap(image, OUTPUT_BMP_WIDTH, OUTPUT_BMP_HEIGHT, true);
        double[][] realImage = new double[image.getHeight()][image.getWidth()];
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                realImage[y][x] = (r + g + b) / 3 + 1;
            }
        return realImage;
    }

    // Function to create the final LBP vector
    private int[] LBPVector(double[][] realImage, int nbx, int nby, int bw, int bh) {
        int samples = 16;
        int max = samples * (samples - 1) + 3;
        int index = 0;
        Vector<Integer> table = new Vector<>((int) Math.pow(2, samples));

        for (int i = 0; i <= (Math.pow(2, samples)) - 1; i++) {
            int shift = (i % 32768) << 1;
            //int shift = (i%128) << 1;
            int position = (i >> (samples - 1));

            int j = shift + position;

            int xor = i ^ j;
            int numt = 0;
            for (int s = 0; s < samples; s++) {
                byte tt = (byte) ((xor >> (s) & 1));
                numt = numt + tt;
            }

            if (numt <= 2) {
                table.add(i, index);
                index += 1;

            } else {
                table.add(i, max - 1);
            }
        }


        int width1 = realImage.length;
        int height1 = realImage.length;

        int[] LBPUTexDesc = new int[max * nbx * nby];
        int[] lim1 = new int[(int) Math.ceil(width1 / bw)];
        int[] lim2 = new int[width1 / bw];
        int[] lim3 = new int[(int) Math.ceil(height1 / bh)];
        int[] lim4 = new int[height1 / bh];

        int cont = 0;
        int conti = 0;
        for (int ii = 0; ii < width1; ii = ii + bw) {
            if (conti < lim1.length)
                lim1[conti] = ii;

            if (conti < lim2.length)
                lim2[conti] = bw + ii - 1;
            conti++;
        }
        conti = 0;
        for (int ii = 0; ii < height1; ii = ii + bh) {
            if (conti < lim3.length)
                lim3[conti] = ii;
            if (conti < lim4.length)
                lim4[conti] = bh + ii - 1;
            conti++;
        }


        for (int ii = 0; ii < nbx; ii++) {
            for (int jj = 0; jj < nby; jj++) {
                double[][] imregion = new double[bw][bh];

                int ci = 0;
                int cj = 0;
                for (int i = lim1[ii]; i <= lim2[ii]; i++) {
                    for (int j = lim3[jj]; j <= lim4[jj]; j++) {
                        imregion[cj][ci] = realImage[j][i];
                        cj++;
                    }
                    cj = 0;
                    ci++;
                }

                int[] finalHist = lbpAux(imregion, table, max, samples);
                if (finalHist == null) {
                    // TODO: What here?
                    return null;
                }
                for (int contador = 0; contador < finalHist.length; contador++) {
                    LBPUTexDesc[contador + cont] = finalHist[contador];
                }
                cont = cont + finalHist.length;

            }
        }
        return LBPUTexDesc;
    }

    // Function to create the LBP vector of each region
    private static int[] lbpAux(double[][] imregion, Vector<Integer> table, int max, int samples) {
        double radius = 2;

        double angle = 2 * Math.PI / samples;

        double[][] points = new double[samples][2];
        double min_x = 0;
        double min_y = 0;
        double max_x = 0;
        double max_y = 0;

        for (int n1 = 0; n1 < samples; n1++) {
            for (int n2 = 0; n2 < 1; n2++) {
                points[n1][n2] = -radius * Math.sin((n1) * angle);
                points[n1][n2 + 1] = radius * Math.cos((n1) * angle);

                if (points[n1][n2] < min_x) {
                    min_x = points[n1][n2];
                }
                if (points[n1][n2 + 1] < min_y) {
                    min_y = points[n1][n2 + 1];
                }
                if (points[n1][n2] > max_x) {
                    max_x = points[n1][n2];
                }
                if (points[n1][n2 + 1] > max_y) {
                    max_y = points[n1][n2 + 1];
                }
            }
        }

        long max_y_round = Math.round(max_y);
        long min_y_round = Math.round(min_y);
        long max_x_round = Math.round(max_x);
        long min_x_round = Math.round(min_x);

        int coord_x = (int) (1 - min_x_round);
        int coord_y = (int) (1 - min_y_round);

        if (imregion.length < (max_y_round - min_y_round + 1) && (imregion.length) < (max_x_round - min_x_round + 1)) {
            System.out.println("Error, image too small");
            // TODO: What here?
            return null;
        }

        double dx = imregion.length - (max_x_round - min_x_round + 1);
        double dy = imregion.length - (max_y_round - min_y_round + 1);


        double[][] C = new double[(int) (dy + 1)][(int) (dx + 1)];

        for (int jj = coord_x - 1; jj <= coord_x + dx - 1; jj++) {
            for (int tt = coord_y - 1; tt <= coord_y + dy - 1; tt++) {
                C[jj - (coord_x - 1)][tt - (coord_y - 1)] = imregion[jj][tt];
            }
        }

        double[][] result = new double[(int) (dy + 1)][(int) (dx + 1)];
        double[][] result2 = new double[(int) (dy + 1)][(int) (dx + 1)];


        int[][] D = new int[(int) (dy + 1)][(int) (dx + 1)];
        for (int i = 0; i < samples; i++) {

            double y = (points[i][0] + coord_y);
            double x = (points[i][1] + coord_x);

            double fy = Math.floor(y);
            double cy = Math.ceil(y);
            double ry = Math.round(y);
            double fx = Math.floor(x);
            double cx = Math.ceil(x);
            double rx = Math.round(x);


            if (Math.abs(x - rx) < Math.pow(10, -6) && Math.abs(y - ry) < Math.pow(10, -6)) {
                double[][] N = new double[(int) (dy + 1)][(int) (dx + 1)];

                for (int jj = (int) rx - 1; jj <= rx + dx - 1; jj++) {
                    for (int tt = (int) ry - 1; tt <= ry + dy - 1; tt++) {
                        N[(int) (tt - (ry - 1))][(int) (jj - (rx - 1))] = imregion[tt][jj];
                    }
                }
                for (int jj = 0; jj < (dy + 1); jj++) {
                    for (int tt = 0; tt < (dx + 1); tt++) {
                        if (N[jj][tt] >= C[jj][tt]) {
                            D[jj][tt] = 1;
                        } else {
                            D[jj][tt] = 0;
                        }
                    }
                }
            } else {

                double ty = y - fy;
                double tx = x - fx;

                double w1 = Math.round((1 - tx) * (1 - ty) * 1000000);
                w1 = w1 / 1000000;
                double w2 = Math.round(tx * (1 - ty) * 1000000);
                w2 = w2 / 1000000;
                double w3 = Math.round((1 - tx) * ty * 1000000);
                w3 = w3 / 1000000;
                double w4 = Math.round((1 - w1 - w2 - w3) * 1000000);
                w4 = w4 / 1000000;

                double[][] N4 = new double[(int) (dy + 1)][(int) (dx + 1)];
                double[][] N1 = new double[(int) (dy + 1)][(int) (dx + 1)];
                double[][] N2 = new double[(int) (dy + 1)][(int) (dx + 1)];
                double[][] N3 = new double[(int) (dy + 1)][(int) (dx + 1)];
                double[][] N = new double[(int) (dy + 1)][(int) (dx + 1)];

                //W1
                for (int jj = (int) fx - 1; jj <= fx + dx - 1; jj++) {
                    for (int tt = (int) fy - 1; tt <= fy + dy - 1; tt++) {
                        N1[(int) (tt - (fy - 1))][(int) (jj - (fx - 1))] = w1 * imregion[tt][jj];
                    }
                }

                //W2
                for (int jj = (int) cx - 1; jj <= cx + dx - 1; jj++) {
                    for (int tt = (int) fy - 1; tt <= fy + dy - 1; tt++) {
                        N2[(int) (tt - (fy - 1))][(int) (jj - (cx - 1))] = w2 * imregion[tt][jj];
                    }
                }

                //W3
                for (int jj = (int) fx - 1; jj <= fx + dx - 1; jj++) {
                    for (int tt = (int) cy - 1; tt <= cy + dy - 1; tt++) {
                        N3[(int) (tt - (cy - 1))][(int) (jj - (fx - 1))] = w3 * imregion[tt][jj];
                    }
                }

                //W4
                for (int jj = (int) cx - 1; jj <= cx + dx - 1; jj++) {
                    for (int tt = (int) cy - 1; tt <= cy + dy - 1; tt++) {
                        N4[(int) (tt - (cy - 1))][(int) (jj - (cx - 1))] = w4 * imregion[tt][jj];
                    }
                }

                for (int jj = 0; jj < (dy + 1); jj++) {
                    for (int tt = 0; tt < (dx + 1); tt++) {
                        //System.out.println(N1[jj][tt]+ " and " +N2[jj][tt] + " and " +N3[jj][tt] +  " and " +N4[jj][tt]);
                        N[jj][tt] = N1[jj][tt] + N2[jj][tt] + N3[jj][tt] + N4[jj][tt];
                        double ex = Math.round(N[jj][tt] * 10000);
                        N[jj][tt] = ex / 10000;
                    }
                }

                for (int jj = 0; jj < (dy + 1); jj++) {
                    for (int tt = 0; tt < (dx + 1); tt++) {
                        if (N[jj][tt] >= C[jj][tt]) {
                            D[jj][tt] = 1;
                        } else {
                            D[jj][tt] = 0;
                        }
                    }
                }

            } //End else

            double v = Math.pow(2, (i));
            for (int jj = 0; jj < (dy + 1); jj++) {
                for (int tt = 0; tt < (dx + 1); tt++) {
                    result[jj][tt] = result[jj][tt] + (v * D[jj][tt]);
                }
            }
        }

        for (int jj = 0; jj < (dy + 1); jj++) {
            for (int tt = 0; tt < (dx + 1); tt++) {
                result2[jj][tt] = table.get((int) (result[jj][tt]));
            }
        }

        int[] finalResult = new int[max];

        for (int jj = 0; jj < (dy + 1); jj++) {
            for (int tt = 0; tt < (dx + 1); tt++) {
                int position = (int) (result2[jj][tt]);
                finalResult[position] = finalResult[position] + 1;
            }
        }

        return finalResult;
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
            detector = null;
        }
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
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

    @Override
    public String requiredPermission() {
        return Manifest.permission.CAMERA;
    }
}