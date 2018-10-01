/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.client.model.Documents;

import javax.ws.rs.core.MediaType;

/**
 * @author dmetzler
 */
public class SimpleClientTest {

    public static final String AUTOMATION_URL = "http://localhost:8080/nuxeo/site/automation";

    private MockedHttpAutomationClient client;

    @Before
    public void doBefore() throws Exception {
        client = new MockedHttpAutomationClient(AUTOMATION_URL);
        client.addOperation("Document.Query").withParams("query");
    }

    @Test
    public void callingAutomationSendsAnHttpRequest() throws Exception {
        client.setResponse(MediaType.APPLICATION_JSON, HttpResponses.DOC_WORKSPACE);

        Session session = client.getSession("Administrator", "Administrator");
        Documents docs = (Documents) session.newRequest("Document.Query").set("query", "SELECT * FROM Document").execute();
        assertTrue(docs.size() > 0);

        client.shutdown();
    }

    @Test
    public void returningADocWitNoPathShouldNotThrowError() throws Exception {
        // NXP-6777
        client.setResponse(MediaType.APPLICATION_JSON, HttpResponses.DOC_NOPATH);

        Session session = client.getSession("Administrator", "Administrator");
        Documents docs = (Documents) session.newRequest("Document.Query").set("query", "SELECT * FROM Document").execute();
        assertTrue(docs.size() > 0);

        client.shutdown();

    }
}
