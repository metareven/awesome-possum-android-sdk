package com.telenor.possumcore;

import android.content.Context;

import com.telenor.possumcore.abstractdetectors.AbstractDetector;
import com.telenor.possumcore.detectors.Accelerometer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class PossumCoreTest {
    private PossumCore possumCore;
    private Accelerometer mockedAccelerometer;

    @Before
    public void setUp() throws Exception {
        mockedAccelerometer = Mockito.mock(Accelerometer.class);
        possumCore = new PossumCore(RuntimeEnvironment.application, "testId") {
            @Override
            protected void addAllDetectors(Context context) {
                addDetector(mockedAccelerometer);
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        possumCore = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(possumCore);
    }

    @Test
    public void testStartListeningWithDetectors() throws Exception {
        Field detectorsField = PossumCore.class.getDeclaredField("detectors");
        detectorsField.setAccessible(true);
        HashSet<AbstractDetector> detectors = (HashSet<AbstractDetector>)detectorsField.get(possumCore);
        Assert.assertTrue(detectors.size() == 1);
        Mockito.verify(mockedAccelerometer, Mockito.times(0)).run();
        Assert.assertTrue(possumCore.startListening());
        Field executorField = PossumCore.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService service = (ExecutorService)executorField.get(possumCore);
        service.awaitTermination(1, TimeUnit.SECONDS);
        Mockito.verify(mockedAccelerometer, Mockito.times(1)).run();
    }

    @Test
    public void testStartListeningWithoutDetectors() throws Exception {
        possumCore = new PossumCore(RuntimeEnvironment.application, "testId") {
            @Override
            protected void addAllDetectors(Context context) {

            }
        };
        Assert.assertFalse(possumCore.startListening());
    }

    @Test
    public void testStartListeningWhileAlreadyListening() throws Exception {
        Assert.assertTrue(possumCore.startListening());
        Assert.assertFalse(possumCore.startListening());
    }

    @Test
    public void testStopListening() throws Exception {
        possumCore.startListening();
        Field listenField = PossumCore.class.getDeclaredField("isListening");
        listenField.setAccessible(true);
        AtomicBoolean listen = (AtomicBoolean)listenField.get(possumCore);
        Assert.assertTrue(listen.get());
        possumCore.stopListening();
        listen = (AtomicBoolean)listenField.get(possumCore);
        Assert.assertFalse(listen.get());
    }

    @Test
    public void testIsListening() throws Exception {
        Assert.assertFalse(possumCore.isListening());
        Field listenField = PossumCore.class.getDeclaredField("isListening");
        listenField.setAccessible(true);
        AtomicBoolean listen = (AtomicBoolean)listenField.get(possumCore);
        listen.set(true);
        Assert.assertTrue(possumCore.isListening());
    }

    @Test
    public void testSetTimeOut() throws Exception {
        Field timeOutField = PossumCore.class.getDeclaredField("timeOut");
        timeOutField.setAccessible(true);
        long timeout = (long)timeOutField.get(possumCore);
        Assert.assertEquals(3000, timeout);
        possumCore.setTimeOut(100);
        timeout = (long)timeOutField.get(possumCore);
        Assert.assertEquals(100, timeout);
    }
}