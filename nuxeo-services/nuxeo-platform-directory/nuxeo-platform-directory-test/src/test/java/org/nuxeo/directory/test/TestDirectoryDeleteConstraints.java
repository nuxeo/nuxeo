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
 *     Guillaume Renard
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryDeleteConstraintException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.DummyNuxeoLoginModule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, ClientLoginFeature.class })
public class TestDirectoryDeleteConstraints {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DirectoryFeature feature;

    @Inject
    protected ClientLoginFeature dummyLogin;

    protected Session continentSession;

    protected Session countrySession;

    @Inject
    protected DirectoryService directoryService;

    @Before
    public void setUp() throws Exception {
        harness.deployContrib(feature.getTestBundleName(), "test-sql-directories-schema-override.xml");
        harness.deployContrib(feature.getTestBundleName(), "test-sql-directories-bundle.xml");
        harness.deployContrib(feature.getTestBundleName(), "test-directory-delete-contrib.xml");
    }

    @Test
    public void testDeleteEntryWithConstraints() throws Exception {

        continentSession = directoryService.getDirectory("continent").getSession();
        countrySession = directoryService.getDirectory("country").getSession();

        // Given the admin user
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);

        // I can delete entry
        DocumentModel entry = continentSession.getEntry("europe");
        Assert.assertNotNull(entry);
        try {
            continentSession.deleteEntry("europe");
            fail("Entry should not be deletable.");
        } catch (DirectoryDeleteConstraintException e) {
            // Expected
            Map<String, Serializable> params = new HashMap<>();
            params.put("parent", "europe");
            for (DocumentModel doc : countrySession.query(params)) {
                countrySession.deleteEntry(doc);
            }
            continentSession.deleteEntry("europe");
        }

        dummyLogin.logout();

        continentSession.close();
        countrySession.close();
    }

}
