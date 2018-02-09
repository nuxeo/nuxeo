/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.DummyNuxeoLoginModule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, ClientLoginFeature.class })
@Deploy({ "org.nuxeo.ecm.directory.tests:intIdDirectory-with-data-contrib.xml" })
public class TestDirectorySecurityAtInitialization {

    private static final String TEST_DIRECTORY = "testIdDirectory";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected ClientLoginFeature dummyLogin;

    protected Session getSession() {
        return directoryService.open(TEST_DIRECTORY);
    }

    @Test
    public void testDirectoryInitialization() throws Exception {
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);
        try (Session session = getSession()) {
            assertEquals(3, session.query(Collections.emptyMap()).size());
        }
        dummyLogin.logout();
        directoryService.unregisterDirectoryDescriptor(directoryService.getDirectoryDescriptor(TEST_DIRECTORY));
        dummyLogin.login("aUser");
        try (Session session = getSession()) {
            assertEquals(3, session.query(Collections.emptyMap()).size());
        }
        dummyLogin.logout();
    }

}
