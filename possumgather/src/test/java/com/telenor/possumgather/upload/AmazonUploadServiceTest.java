package com.telenor.possumgather.upload;

import android.content.Intent;

import com.telenor.possumgather.BuildConfig;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class AmazonUploadServiceTest {
    private AmazonUploadService service;
    @Before
    public void setUp() throws Exception {
        service = new AmazonUploadService();
    }

    @After
    public void tearDown() throws Exception {
        service = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(service);
    }

    @Test
    public void testBindReturnsNull() throws Exception {
        Assert.assertNull(service.onBind(new Intent()));
    }
}