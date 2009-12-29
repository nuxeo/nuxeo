package org.nuxeo.ecm.platform.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;

import com.google.inject.Inject;

@RunWith(NuxeoPlatformRunner.class)
public class UserManagerTest {
    @Inject UserManager um;

    @Test
    public void userManagerIsInjected() throws Exception {
        assertNotNull(um);
        assertEquals(UserManagerImpl.class, um.getClass());
    }

    @Test
    public void testUsersAreHere() throws Exception {
        assertNotNull(um.getPrincipal("Administrator"));
    }

}
