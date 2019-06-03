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
 *     mhilaire
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml")
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-bundle.xml")
public class TestDirectorySecurityDefault {

    @Inject
    protected DirectoryService directoryService;

    public Session getSession() {
        return directoryService.open(DirectoryFeature.USER_DIRECTORY_NAME);
    }

    // Default admin tests
    @Test
    @WithUser("Administrator")
    public void adminCanCreateEntry() {
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("groups", Arrays.asList("members", "administrators"));

            // When I call the create entry
            DocumentModel entry = session.createEntry(map);

            // I have created an entry
            entry = session.getEntry(entry.getId());
            Assert.assertNotNull(entry);
        }
    }

    @Test
    @WithUser("Administrator")
    public void adminCanDeleteEntry() {
        try (Session session = getSession()) {
            // I can delete entry
            DocumentModel entry = session.getEntry("user_1");
            Assert.assertNotNull(entry);
            session.deleteEntry("user_1");
            entry = session.getEntry("user_1");
            Assert.assertNull(entry);
        }
    }

    // Everyone tests
    @Test
    @WithUser("aUser")
    public void everyoneCantCreateEntry() {
        try (Session session = getSession()) {
            // Given a user

            Map<String, Object> map = new HashMap<>();
            map.put("username", "should-not-create");
            map.put("password", "should-not-create");
            map.put("groups", Arrays.asList("members", "administrators"));

            // When I call the create entry
            session.createEntry(map);
            fail("Should not be able to create entry");

        } catch (DirectorySecurityException e) {
            // ok
        }
    }

    @Test
    @WithUser("aUser")
    public void everyoneCanGetEntry() {
        try (Session session = getSession()) {
            // Given a user

            // When I call get entry
            DocumentModel entry = session.getEntry("user_3");
            Assert.assertNotNull(entry);
        }
    }

    @Test
    @WithUser("aUser")
    public void everyoneCantDeleteEntry() {
        try (Session session = getSession()) {

            // When I call delete entry
            DocumentModel entry = session.getEntry("user_3");
            Assert.assertNotNull(entry);
            session.deleteEntry("user_3");
            fail("Should not be able to delete entry");

            entry = session.getEntry("user_3");
            Assert.assertNotNull(entry);

        } catch (DirectorySecurityException e) {
            // ok
        }
    }

    @Test
    @WithUser("aUser")
    public void everyoneCanSearch() {
        try (Session session = getSession()) {

            // When I query entry
            Map<String, Serializable> map = new HashMap<>();
            map.put("username", "user_3");
            DocumentModelList results = session.query(map);
            Assert.assertNotNull(results);
            Assert.assertEquals(1, results.size());
        }
    }

}
