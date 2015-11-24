package org.nuxeo.ecm.spaces.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.opensocial.spaces",
    "org.nuxeo.ecm.platform.picture.core",
    "org.nuxeo.ecm.opensocial.spaces.test:OSGI-INF/test-spacepermissions-contrib.xml" })
public class SpacePermissionsTest {

    @Inject
    SpaceManager sm;

    @Test
    public void iCanGetAllPermissions() throws Exception {
        List<String> permissions = sm.getAvailablePermissions();
        assertEquals(3, permissions.size());
    }

    @Test
    public void iCanGetPermissionsByName() throws Exception {
        assertTrue(sm.getAvailablePermissions().contains("Write"));
        assertTrue(sm.getAvailablePermissions().contains("Write1"));
        assertTrue(sm.getAvailablePermissions().contains("Write2"));
        assertFalse(sm.getAvailablePermissions().contains("notDefined"));
    }
}
