package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.google.inject.Inject;


@RunWith(NuxeoRunner.class)
public class UserManagerHarnessTest {

    @Inject
    private UserManager service;

    @Test
    public void serviceIsInjected() throws Exception {
        assertNotNull(service);
    }

    @Test
    public void canRetrieveTheAdminAccount() throws Exception {
        DocumentModel admin = service.getUserModel("Administrator");
        assertNotNull(admin);
    }

}
