/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateProxyLive;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/** @since 2021.17 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
public class TestProxyOperations {
    
    @Inject
    protected Session httpSession;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected HttpAutomationClient client;

    // NXP-30913
    @Test
    public void testProxyCreationRestriction() throws IOException {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "testUser");
        user.setPropertyValue("user:password", "secret");
        userManager.createUser(user);

        httpSession.newRequest(CreateDocument.ID).setInput("/").set("type", "File").set("name", "test").execute();
        txFeature.nextTransaction();

        // Administrator OK
        httpSession.newRequest(CreateProxyLive.ID).setInput("/test").set("Destination Path", "/").execute();

        // simple user KO
        try {
            httpSession = client.getSession("testUser", "secret");
            httpSession.newRequest(CreateProxyLive.ID).setInput("/test").set("Destination Path", "/").execute();
            fail("request is supposed to return " + SC_NOT_FOUND);
        } catch (RemoteException e) {
            assertEquals(SC_NOT_FOUND, e.getStatus());
        }
    }

}
