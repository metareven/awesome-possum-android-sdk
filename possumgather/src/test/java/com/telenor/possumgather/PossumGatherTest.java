package com.telenor.possumgather;

import android.content.Context;

import com.telenor.possumgather.mockedDetectors.MockedAccelerometer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PossumGatherTest {
    @Mock
    private MockedAccelerometer mockedAccelerometer;

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

    @Test
    public void testIsUploading() throws Exception {
        Assert.assertFalse(possumGather.isUploading(RuntimeEnvironment.application));
        // TODO: Test while uploading as well
    }
}