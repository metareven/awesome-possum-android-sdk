package com.telenor.possumcore.abstractdetectors;

import com.google.gson.JsonArray;
import com.telenor.possumcore.TestUtils;
import com.telenor.possumcore.interfaces.IDetectorChange;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class AbstractDetectorTest {
    private AbstractDetector abstractDetector;
    private IDetectorChange detectorChange;

    @Before
    public void setUp() throws Exception {
        TestUtils.initializeJodaTime();
        detectorChange = Mockito.mock(IDetectorChange.class);
        abstractDetector = new AbstractDetector(RuntimeEnvironment.application, detectorChange) {
            @Override
            public int detectorType() {
                return 999;
            }

            @Override
            public String detectorName() {
                return "abstractDetector";
            }

            @Override
            public void terminate() {

            }
        };
    }

    @After
    public void tearDown() throws Exception {
        abstractDetector = null;
    }

    @Test
    public void testInitialize() throws Exception {
        Assert.assertNotNull(abstractDetector);
        Assert.assertEquals("abstractDetector", abstractDetector.detectorName());
        Assert.assertEquals(999, abstractDetector.detectorType());
        Assert.assertNotNull(abstractDetector.context());
    }

    @Test
    public void testInvalidArgument() throws Exception {
        try {
            abstractDetector = new AbstractDetector(null, null) {
                @Override
                public int detectorType() {
                    return 999;
                }

                @Override
                public String detectorName() {
                    return "abstractDetector";
                }

                @Override
                public void terminate() {

                }
            };
            Assert.fail("Should not pass without context");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Missing context", e.getMessage());
        }
    }

    @Test
    public void testNowAndJoda() throws Exception {
        try {
            long present = System.currentTimeMillis();
            long timestamp = abstractDetector.now();
            Assert.assertTrue(timestamp >= present);
        } catch (Exception e) {
            Assert.fail("Joda time is not initialized");
        }
    }

    @Test
    public void testIsAvailable() throws Exception {
        Assert.assertNull(abstractDetector.requiredPermission());
        Assert.assertTrue(abstractDetector.isPermitted());
        Assert.assertTrue(abstractDetector.isAvailable());
    }

    @Test
    public void testIsEnabledByDefault() throws Exception {
        Assert.assertTrue(abstractDetector.isEnabled());
    }

    @Test
    public void testLongScanIsFalseByDefault() throws Exception {
        Assert.assertFalse(abstractDetector.isLongScanDoable());
    }

    @Test
    public void testNothingIsStoredWhenAttemptingToStoreOnInvalidDataSet() throws Exception {
        abstractDetector.streamData(new JsonArray(), "invalidSet");
        Field dataStoredField = AbstractDetector.class.getDeclaredField("dataStored");
        dataStoredField.setAccessible(true);
        Map<String, List<JsonArray>> data = (Map<String, List<JsonArray>>)dataStoredField.get(abstractDetector);
        Assert.assertTrue(data.get("invalidSet") == null);
    }

    @Test
    public void testInvalidDataSetReturnsNull() throws Exception {
        Assert.assertNull(abstractDetector.dataSet("invalidSet"));
    }

    @Test
    public void testNoPermissionRequiredInAbstraction() throws Exception {
        Assert.assertNull(abstractDetector.requiredPermission());
    }

    @Test
    public void testPermittedReturnsFalseIfNotGranted() throws Exception {
        abstractDetector = new AbstractDetector(RuntimeEnvironment.application, detectorChange) {
            @Override
            public int detectorType() {
                return 999;
            }
            @Override
            protected String requiredPermission() {
                return "android.permission.ACCESS_FINE_LOCATION";
            }

            @Override
            public String detectorName() {
                return "testDetector";
            }

            @Override
            public void terminate() {

            }
        };
        Assert.assertEquals("android.permission.ACCESS_FINE_LOCATION", abstractDetector.requiredPermission());
        Assert.assertFalse(abstractDetector.isPermitted());
    }

    @Test
    public void testPermittedReturnsTrueIfGranted() throws Exception {
        abstractDetector = new AbstractDetector(RuntimeEnvironment.application, detectorChange) {
            @Override
            public int detectorType() {
                return 999;
            }
            @Override
            protected String requiredPermission() {
                return "android.permission.ACCESS_FINE_LOCATION";
            }

            @Override
            public String detectorName() {
                return "testDetector";
            }

            @Override
            public void terminate() {

            }
        };
        ShadowApplication.getInstance().grantPermissions("android.permission.ACCESS_FINE_LOCATION");
        Assert.assertTrue(abstractDetector.isPermitted());
    }

    @Test
    public void testUniqueUserId() throws Exception {
        Field uniqueUserField = AbstractDetector.class.getDeclaredField("uniqueUserId");
        uniqueUserField.setAccessible(true);
        String presentUserId = (String) uniqueUserField.get(abstractDetector);
        Assert.assertNull(presentUserId);
        abstractDetector.setUniqueUserId("testUserId");
        presentUserId = (String) uniqueUserField.get(abstractDetector);
        Assert.assertEquals("testUserId", presentUserId);
    }

    @Test
    public void testRunClearsDataButNotDataSets() throws Exception {
        Field dataField = AbstractDetector.class.getDeclaredField("dataStored");
        dataField.setAccessible(true);
        Map<String, List<JsonArray>> data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        List<JsonArray> array = new ArrayList<>();
        array.add(new JsonArray());
        data.put("default", array);
        data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        Assert.assertTrue(data.size() == 1);
        Assert.assertTrue(data.get("default").size() == 1);
        abstractDetector.run();
        data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        Assert.assertTrue(data.size() == 1);
        Assert.assertTrue(data.get("default").size() == 0);
    }

    @Test
    public void testStreamDataAddsToDataStored() throws Exception {
        Field dataField = AbstractDetector.class.getDeclaredField("dataStored");
        dataField.setAccessible(true);
        abstractDetector.streamData(new JsonArray());
        Map<String, List<JsonArray>> data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, data.get("default").size());
        abstractDetector.streamData(new JsonArray());
        data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(2, data.get("default").size());
    }

    @Test
    public void testUserId() throws Exception {
        Assert.assertNull(abstractDetector.getUserId());
        abstractDetector.setUniqueUserId("testId");
        Assert.assertEquals("testId", abstractDetector.getUserId());
    }

    @Test
    public void testChangeInStatus() throws Exception {
        abstractDetector.detectorStatusChanged();
    }

    @Test
    public void testDataStored() throws Exception {
        Assert.assertEquals(1, abstractDetector.dataStored().size());
        Assert.assertEquals("default", abstractDetector.dataStored().keySet().iterator().next());
        Assert.assertEquals(0, abstractDetector.dataStored().get("default").size());
        Assert.assertNull(abstractDetector.dataStored().get("unknown"));
        Field dataField = AbstractDetector.class.getDeclaredField("dataStored");
        dataField.setAccessible(true);
        Map<String, List<JsonArray>> data = (Map<String, List<JsonArray>>)dataField.get(abstractDetector);
        data.get("default").add(new JsonArray());
        Assert.assertEquals(1, abstractDetector.dataStored().get("default").size());
    }

    @Test
    public void testCleanUpForCompletion() throws Exception {
        abstractDetector.cleanUp();
    }
}