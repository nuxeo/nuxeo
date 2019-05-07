/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.directory.DirectoryDeleteConstraintException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, ClientLoginFeature.class })
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml")
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-bundle.xml")
@Deploy("org.nuxeo.ecm.directory.tests:test-directory-delete-contrib.xml")
@WithUser("Administrator")
public class TestDirectoryDeleteConstraints {

    protected Session continentSession;

    protected Session countrySession;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testDeleteEntryWithConstraints() {

        continentSession = directoryService.getDirectory("continent").getSession();
        countrySession = directoryService.getDirectory("country").getSession();

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

        continentSession.close();
        countrySession.close();
    }

}
