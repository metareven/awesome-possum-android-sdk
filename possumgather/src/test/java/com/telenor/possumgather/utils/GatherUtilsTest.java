package com.telenor.possumgather.utils;

import com.telenor.possumgather.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class GatherUtilsTest {
    @Before
    public void setUp() throws Exception {
    }
    @After
    public void tearDown() throws Exception {

    }
    @Test
    public void testGetFiles() throws Exception {
        //GatherUtils.getFiles()
    }
    @Test
    public void testCreateZipStream() throws Exception {
        //GatherUtils.createZipStream()
    }
    @Test
    public void testStorageCatalogue() throws Exception {
        //GatherUtils.storageCatalogue()
    }
}