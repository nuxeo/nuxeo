/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.signature.web;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.web.sign.CertActions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api", "org.nuxeo.runtime.management", "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.signature.core", "org.nuxeo.ecm.platform.signature.web",
        "org.nuxeo.ecm.platform.signature.web.test" })
public class CertActionsTest {

    private static final String USER_ID = "hsimpson";

    protected DocumentModel user;

    protected CertActions certActions;

    @Before
    public void setup() throws Exception {
        certActions = new CertActions();
    }

    @Test
    public void testValidateRequiredUserFields() throws Exception {
        DocumentModel user = getFullUser();
        assertNotNull("User not created", user);
        // certActions.validateRequiredUserFields();
    }

    public DocumentModel getFullUser() throws Exception {
        if (user == null) {
            user = getUserManager().getUserModel(USER_ID);
            if (user == null) {
                DocumentModel userModel = getUserManager().getBareUserModel();
                userModel.setProperty("user", "username", USER_ID);
                userModel.setProperty("user", "firstName", "Homer");
                userModel.setProperty("user", "lastName", "Simpson");
                userModel.setProperty("user", "email", "simps@on.com");
                userModel.setPathInfo("/", USER_ID);
                user = getUserManager().createUser(userModel);
            }
        }
        return user;
    }

    protected static UserManager getUserManager() {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        assertNotNull(userManager);
        return userManager;
    }
}
