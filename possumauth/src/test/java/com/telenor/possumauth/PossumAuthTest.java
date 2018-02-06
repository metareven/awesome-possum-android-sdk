package com.telenor.possumauth;

import com.telenor.possumcore.TestUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class PossumAuthTest {
    private PossumAuth possumAuth;

    @Before
    public void setUp() throws Exception {
        TestUtils.initializeJodaTime();
        possumAuth = new PossumAuth(RuntimeEnvironment.application, "testUser", "fakeUploadUrl");
    }

    @After
    public void tearDown() throws Exception {
        possumAuth = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(possumAuth);
    }

    @Test
    public void testDetectorsAreAllThere() throws Exception {
        Assert.assertEquals(8, possumAuth.detectors().size());
    }
}