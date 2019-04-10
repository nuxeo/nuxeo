/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.core.SignatureCoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(SignatureCoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.api")
public class CUserServiceTest {

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    private static final String USER_ID = "hsimpson";

    @Inject
    protected CUserService cUserService;

    @Inject
    protected UserManager userManager;

    protected DocumentModel user;

    @Before
    public void setUp() {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", USER_ID);
        userModel.setProperty("user", "firstName", "Homer");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "simps@on.com");
        userModel.setPathInfo("/", USER_ID);
        user = userManager.createUser(userModel);
    }

    public void testCreateCert() {
        DocumentModel certificate = cUserService.createCertificate(user, USER_KEYSTORE_PASSWORD);
        assertTrue(certificate.getPropertyValue("cert:userid").equals(USER_ID));
    }

    @Test
    public void testGetCertificate() {
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
