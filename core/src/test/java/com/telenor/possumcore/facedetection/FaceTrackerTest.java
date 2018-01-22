package com.telenor.possumcore.facedetection;

import com.telenor.possumcore.BuildConfig;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class FaceTrackerTest {
    private FaceTracker faceTracker;

    @Before
    public void setUp() throws Exception {
        faceTracker = new FaceTracker();
    }

    @After
    public void tearDown() throws Exception {
        faceTracker = null;
    }

    @Test
    public void testInitialize() throws Exception {
        faceTracker.onDone();
        faceTracker.onMissing(null);
        faceTracker.onNewItem(0, null);
        faceTracker.onUpdate(null, null);
        Assert.assertNotNull(faceTracker);
    }
}