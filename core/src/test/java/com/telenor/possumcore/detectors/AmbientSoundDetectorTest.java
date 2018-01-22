package com.telenor.possumcore.detectors;

import android.Manifest;
import android.media.AudioManager;

import com.telenor.possumcore.BuildConfig;
import com.telenor.possumcore.constants.DetectorType;
import com.telenor.possumcore.shadows.ShadowAudioRecord;
import com.telenor.possumcore.shadows.ShadowAudioTrack;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowAudioManager;

import java.lang.reflect.Field;

@Config(constants = BuildConfig.class, shadows = {ShadowAudioManager.class, ShadowAudioTrack.class, ShadowAudioRecord.class})
@RunWith(RobolectricTestRunner.class)
public class AmbientSoundDetectorTest {
    private AmbientSoundDetector ambientSoundDetector;
    private ShadowAudioManager shadowAudioManager;

    @Before
    public void setUp() throws Exception {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.RECORD_AUDIO);
        ambientSoundDetector = new AmbientSoundDetector(RuntimeEnvironment.application);
        Field audioManagerField = AmbientSoundDetector.class.getDeclaredField("audioManager");
        audioManagerField.setAccessible(true);
        shadowAudioManager = Shadows.shadowOf((AudioManager)audioManagerField.get(ambientSoundDetector));
    }

    @After
    public void tearDown() throws Exception {
        ambientSoundDetector = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(ambientSoundDetector);
        Assert.assertEquals(DetectorType.Audio, ambientSoundDetector.detectorType());
        Assert.assertEquals("sound", ambientSoundDetector.detectorName());
        Assert.assertEquals(Manifest.permission.RECORD_AUDIO, ambientSoundDetector.requiredPermission());
    }

    @Test
    public void testEnabled() throws Exception {
        Assert.assertTrue(ambientSoundDetector.isEnabled());
        Field audioManagerField = AmbientSoundDetector.class.getDeclaredField("audioManager");
        audioManagerField.setAccessible(true);
        audioManagerField.set(ambientSoundDetector, null);
        Assert.assertFalse(ambientSoundDetector.isEnabled());
    }

    @Test
    public void testAvailable() throws Exception {
        Assert.assertTrue(ambientSoundDetector.isAvailable());
        ShadowApplication.getInstance().denyPermissions(Manifest.permission.RECORD_AUDIO);
        Assert.assertFalse(ambientSoundDetector.isAvailable());
    }
    @Test
    public void testRunWhenMuted() throws Exception {
        shadowAudioManager.setMicrophoneMute(true);
    }
}