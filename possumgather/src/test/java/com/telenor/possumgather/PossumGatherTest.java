package com.telenor.possumgather;

import android.content.Context;

import com.telenor.possumcore.TestUtils;
import com.telenor.possumgather.detectors.Accelerometer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class PossumGatherTest {
    @Mock
    private Accelerometer mockedAccelerometer;

    private PossumGather possumGather;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        TestUtils.initializeJodaTime();
        possumGather = new PossumGather(RuntimeEnvironment.application, "testUser") {
            @Override
            protected void addAllDetectors(Context context) {
                addDetector(mockedAccelerometer);
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        possumGather = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(possumGather);
    }
}