package com.telenor.possumcore.detectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;

import com.telenor.possumcore.BuildConfig;
import com.telenor.possumcore.TestUtils;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.neuralnetworks.TensorWeights;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowCamera;
import org.robolectric.shadows.ShadowPackageManager;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Config(constants = BuildConfig.class) //, abiSplit = "aarch64"
@RunWith(RobolectricTestRunner.class)
public class ImageDetectorTest {
    @Mock
    private Context mockedContext;
    @Mock
    private TensorWeights mockedTensor;
    @Mock
    private CameraManager mockedCameraManager;

    private ImageDetector imageDetector;
    private int counter;
    private ShadowCamera frontCameraShadow;
    private ShadowCamera backCameraShadow;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        TestUtils.initializeJodaTime();
        counter = 0;
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(RuntimeEnvironment.application.getPackageManager());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, true);
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, true);
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.CAMERA);
//        Assert.fail("Architecture:"+System.getProperty("os.arch"));
        when(mockedContext.checkPermission(eq(Manifest.permission.CAMERA), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED);
        imageDetector = new ImageDetector(RuntimeEnvironment.application) {
            @Override
            protected CameraManager cameraManager(Context context) {
               return mockedCameraManager;
            }
            @Override
            protected TensorWeights createTensor(AssetManager assetManager, String modelName) {
                return mockedTensor;
            }
            @Override
            protected void initializeOpenCV() {
                // To avoid dealing with static library loading in robolectric, openCV is removed
                // from initialization of detector
                counter++;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        imageDetector = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(imageDetector);
        Assert.assertEquals(1, counter); // Confirms openCV is initialized
        Assert.assertEquals("image", imageDetector.detectorName());
        Assert.assertEquals(DetectorType.Image, imageDetector.detectorType());
        Assert.assertEquals(Manifest.permission.CAMERA, imageDetector.requiredPermission());
        Field cameraSourceField = ImageDetector.class.getDeclaredField("cameraSource");
        cameraSourceField.setAccessible(true);
        Assert.assertNotNull(cameraSourceField.get(imageDetector));
    }

    @Test
    public void testEnabled() throws Exception {
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(RuntimeEnvironment.application.getPackageManager());
        Assert.assertTrue(imageDetector.isEnabled());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, false);
        Assert.assertFalse(imageDetector.isEnabled());
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, true);
        Assert.assertTrue(imageDetector.isEnabled());
        Field tensorFlowField = ImageDetector.class.getDeclaredField("tensorFlowInterface");
        tensorFlowField.setAccessible(true);
        tensorFlowField.set(imageDetector, null);
        Assert.assertFalse(imageDetector.isEnabled());
    }

    @Test
    public void testCameraNotInUse() throws Exception {
        Assert.assertFalse(imageDetector.isCameraUsed());
        Field availabilityCallbackField = ImageDetector.class.getDeclaredField("availabilityCallback");
        availabilityCallbackField.setAccessible(true);
        CameraManager.AvailabilityCallback callback = (CameraManager.AvailabilityCallback)availabilityCallbackField.get(imageDetector);
        callback.onCameraUnavailable(""+Camera.CameraInfo.CAMERA_FACING_FRONT);
        Assert.assertTrue(imageDetector.isCameraUsed());
        callback.onCameraAvailable(""+Camera.CameraInfo.CAMERA_FACING_FRONT);
        Assert.assertFalse(imageDetector.isCameraUsed());
        callback.onCameraUnavailable(""+Camera.CameraInfo.CAMERA_FACING_BACK);
        Assert.assertFalse(imageDetector.isCameraUsed());
    }

/*    @Test
    public void testAffineTransform() throws Exception {
        PointF leftEye = new PointF(30, 30);
        PointF rightEye = new PointF(60, 30);
        PointF mouth = new PointF(45, 45);
        Matrix matrix = ImageDetector.affineTransform(96, 96, leftEye, rightEye, mouth);
        float[] values = new float[9];

        matrix.getValues(values);
        Assert.assertEquals(-1.378228759765625, values[0], 0);
        Assert.assertEquals(0.053024037679036455, values[1], 0);
        Assert.assertEquals(107.65379333496094, values[2], 0);
        Assert.assertEquals(0.026751518249511955, values[3], 0);
        Assert.assertEquals(3.7801063537597654, values[4], 0);
        Assert.assertEquals(-99.25249767303468, values[5], 0);
    }*/
    /*
    01-12 14:19:25.575 32543-32543/com.telenor.possumexample I/AP:: src:[{30.0, 30.0}, {60.0, 30.0}, {45.0, 45.0}]
01-12 14:19:25.576 32543-32543/com.telenor.possumexample I/AP:: dest:[{67.89765167236328, 14.953238487243652}, {26.55078887939453, 15.755784034729004}, {48.01958084106445, 72.05610656738281}]
01-12 14:19:25.577 32543-32543/com.telenor.possumexample I/AP:: Row:0
01-12 14:19:25.577 32543-32543/com.telenor.possumexample I/AP:: Col:0 - val:[-1.378228759765625]
01-12 14:19:25.577 32543-32543/com.telenor.possumexample I/AP:: Col:1 - val:[0.053024037679036455]
01-12 14:19:25.578 32543-32543/com.telenor.possumexample I/AP:: Col:2 - val:[107.65379333496094]
01-12 14:19:25.578 32543-32543/com.telenor.possumexample I/AP:: Row:1
01-12 14:19:25.578 32543-32543/com.telenor.possumexample I/AP:: Col:0 - val:[0.026751518249511955]
01-12 14:19:25.578 32543-32543/com.telenor.possumexample I/AP:: Col:1 - val:[3.7801063537597654]
01-12 14:19:25.578 32543-32543/com.telenor.possumexample I/AP:: Col:2 - val:[-99.25249767303468]
    */
/*
cv::Mat cv::getAffineTransform( const Point2f src[], const Point2f dst[] ) {
    Mat M(2, 3, CV_64F), X(6, 1, CV_64F, M.ptr());
    double a[6*6], b[6];
    Mat A(6, 6, CV_64F, a), B(6, 1, CV_64F, b);

    for( int i = 0; i < 3; i++ ) {
        int j = i*12;
        int k = i*12+6;
        a[j] = a[k+3] = src[i].x;
        a[j+1] = a[k+4] = src[i].y;
        a[j+2] = a[k+5] = 1;
        a[j+3] = a[j+4] = a[j+5] = 0;
        a[k] = a[k+1] = a[k+2] = 0;
        b[i*2] = dst[i].x;
        b[i*2+1] = dst[i].y;
    }

    solve( A, B, X );
    return M;
}
*/
}