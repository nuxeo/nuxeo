/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.client.model.Documents;

/**
 * @author dmetzler
 *
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
         client.setResponse("application/json+nxentity", HttpResponses.DOC_WORKSPACE);

         Session session = client.getSession("Administrator", "Administrator");
         Documents docs = (Documents) session.newRequest("Document.Query").set(
                "query", "SELECT * FROM Document").execute();
         assertTrue(docs.size()> 0);

         client.shutdown();
    }


    @Test
    public void returningADocWitNoPathShouldNotThrowError() throws Exception {
        //NXP-6777
        client.setResponse("application/json+nxentity", HttpResponses.DOC_NOPATH);

        Session session = client.getSession("Administrator", "Administrator");
        Documents docs = (Documents) session.newRequest("Document.Query").set(
               "query", "SELECT * FROM Document").execute();
        assertTrue(docs.size()> 0);

        client.shutdown();

    }
}
