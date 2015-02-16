/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Wojciech Sulejman
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.signature.core.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD, user = "Administrator")
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api", "org.nuxeo.runtime.management",
        "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.signature.core", "org.nuxeo.ecm.platform.signature.core.test" })
public class CUserServiceTest {

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    private static final String USER_ID = "hsimpson";

    @Inject
    protected CUserService cUserService;

    @Inject
    protected UserManager userManager;

    protected DocumentModel user;

    @Before
    public void setUp() throws Exception {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", USER_ID);
        userModel.setProperty("user", "firstName", "Homer");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "simps@on.com");
        userModel.setPathInfo("/", USER_ID);
        user = userManager.createUser(userModel);
    }

    public void testCreateCert() throws Exception {
        DocumentModel certificate = cUserService.createCertificate(user, USER_KEYSTORE_PASSWORD);
        assertTrue(certificate.getPropertyValue("cert:userid").equals(USER_ID));
    }

    @Test
    public void testGetCertificate() throws Exception {
        // try to retrieve a certificate that does not yet exist
        DocumentModel retrievedCertificate = cUserService.getCertificate(USER_ID);
        assertNull(retrievedCertificate);
        // add missing certificate
        DocumentModel createdCertificate = cUserService.createCertificate(user, USER_KEYSTORE_PASSWORD);
        assertNotNull(createdCertificate);
        // retry
        retrievedCertificate = cUserService.getCertificate(USER_ID);
        assertNotNull("The certificate could not be retrieved from the directory", retrievedCertificate);
        assertTrue(retrievedCertificate.getPropertyValue("cert:userid").equals(USER_ID));
    }

}