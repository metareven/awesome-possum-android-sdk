package com.telenor.possumcore.constants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

public class DetectorTypeTest {
    @Test
    public void testValues() throws Exception {
        DetectorType type = new DetectorType();
        Assert.assertNotNull(type);
        Assert.assertEquals(1, DetectorType.Accelerometer);
        Assert.assertEquals(4, DetectorType.Gyroscope);
        Assert.assertEquals(100, DetectorType.Network);
        Assert.assertEquals(101, DetectorType.Bluetooth);
        Assert.assertEquals(102, DetectorType.Position);
        Assert.assertEquals(103, DetectorType.Image);
        Assert.assertEquals(104, DetectorType.Audio);
    }
}