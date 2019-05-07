/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features(CoreDirectoryFeature.class)
public class TestCoreDirectoryUserRestricted {

    @Inject
    @Named(value = CoreDirectoryFeature.CORE_DIRECTORY_NAME)
    protected Directory coreDir;

    @Inject
    ClientLoginFeature login;

    protected Session dirRestrictedSession = null;

    @Before
    public void setUp() throws Exception {
        login.login(CoreDirectoryFeature.USER2_NAME);
        dirRestrictedSession = coreDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        try {
            dirRestrictedSession.close();
        } finally {
            login.logout();
        }
    }

    @Test
    public void testGetEntry() {
        DocumentModel entry;
        entry = dirRestrictedSession.getEntry(CoreDirectoryInit.DOC_ID_USER2);
        assertEquals("foo2", entry.getPropertyValue(TestCoreDirectory.FOO_FIELD));
        entry = dirRestrictedSession.getEntry("no-such-entry");
        assertNull(entry);
        entry = dirRestrictedSession.getEntry(CoreDirectoryInit.DOC_ID_USER1);
        assertNull(entry);
    }

}
