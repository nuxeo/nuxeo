/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ftest.server;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;
import static org.nuxeo.functionaltests.AbstractTest.TEST_PASSWORD;
import static org.nuxeo.functionaltests.AbstractTest.TEST_USERNAME;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.spi.NuxeoClientRemoteException;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.functionaltests.RestHelper;

/**
 * Tests the secured property handled by {@link PropertyCharacteristicHandler} and contributed by
 * {@code CoreExtension.xml}.
 *
 * @since 11.1
 */
public class ITSecuredPropertyTest {

    @BeforeClass
    public static void beforeClass() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
    }

    @AfterClass
    public static void afterClass() {
        RestHelper.cleanupUsers();
    }

    @Test
    public void testAdministratorCanEdit() {
        NuxeoClient client = new NuxeoClient.Builder().url(NUXEO_URL)
                                                      .authentication(ADMINISTRATOR, ADMINISTRATOR)
                                                      .connect();
        Document document = Document.createWithName("file", "File");
        document.setPropertyValue("dc:creator", "john");
        client.repository().createDocumentByPath("/", document); // should succeed
    }

    @Test
    public void testUserCanNotEdit() {
        NuxeoClient client = new NuxeoClient.Builder().url(NUXEO_URL)
                                                      .authentication(TEST_USERNAME, TEST_PASSWORD)
                                                      .connect();
        Document document = Document.createWithName("file", "File");
        document.setPropertyValue("dc:creator", "john");
        try {
            client.repository().createDocumentByPath("/", document);
            fail("User " + TEST_USERNAME + " shouldn't have right to edit dc:creator");
        } catch (NuxeoClientRemoteException e) {
            assertEquals(SC_BAD_REQUEST, e.getStatus());
        }
    }

}
