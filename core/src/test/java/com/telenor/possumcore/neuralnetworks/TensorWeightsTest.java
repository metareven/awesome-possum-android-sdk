package com.telenor.possumcore.neuralnetworks;

import android.content.res.AssetManager;

import com.telenor.possumcore.BuildConfig;

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
import org.robolectric.shadows.ShadowAssetManager;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class TensorWeightsTest {
    // TODO: Find a way to test this with unit tests. Problem:
    // "Emulator", robolectric, fires a x86_64 jdk to run tests, but tensorFlow does not support
    // this architecture - only armeabi-v7a
    // (Check https://github.com/miyosuda/TensorFlowAndroidDemo/tree/master/app/src/main/jniLibs/)
    // This makes test impossible unless I can get tensorFlow
    // to run in another architecture OR make robolectric run in a different architecture. So yeah,
    // make tensorFlow run in x86_64 or bust. Perhaps build it with that architecture and include
    // .so file into test?
    @Mock
    private AssetManager mockedAssetManager;
    private TensorWeights tensorWeights;

    private static final String fakeModelName = "fakeModel.pb";
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
//        Assert.fail(System.getProperty("os.arch"));
//        tensorWeights = new TensorWeights(mockedAssetManager, fakeModelName);
    }

    @After
    public void tearDown() throws Exception {
        tensorWeights = null;
    }

    @Test
    public void testInitialize() throws Exception {
//        Assert.assertNotNull(tensorWeights);
    }

    @Test
    public void testGetWeights() throws Exception {
        // TODO: Implement

    }
}