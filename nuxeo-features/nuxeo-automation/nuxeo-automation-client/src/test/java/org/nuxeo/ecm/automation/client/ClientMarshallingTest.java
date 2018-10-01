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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.client.model.Document;

import javax.ws.rs.core.MediaType;

/**
 * @author bjalon
 */
public class ClientMarshallingTest {

    public static final String AUTOMATION_URL = "http://localhost:8080/nuxeo/site/automation";

    private MockedHttpAutomationClient client;

    @Before
    public void doBefore() throws Exception {
        client = new MockedHttpAutomationClient(AUTOMATION_URL);
        client.addOperation("Document.Fetch").withParams("value");
    }

    @Test
    public void shouldFetchDocumentProperties() throws Exception {
        client.setResponse(MediaType.APPLICATION_JSON, HttpResponses.DOC_DEFAULT_DOMAIN);

        Session session = client.getSession("Administrator", "Administrator");
        Document defaultDomain = (Document) session.newRequest("Document.Fetch").set("value", "/default-domain").execute();
        assertEquals("6e4ee4b8-af3f-4fb4-ad31-1a0a88720dfb", defaultDomain.getId());
        assertEquals(1368704100560L, defaultDomain.getLastModified().getTime());
        assertEquals(2, defaultDomain.getFacets().size());
        assertEquals("SuperSpace", defaultDomain.getFacets().getString(0));
        assertEquals("Folderish", defaultDomain.getFacets().getString(1));
        assertEquals(null, defaultDomain.getLock());
        assertEquals(null, defaultDomain.getLockCreated());
        assertEquals(false, defaultDomain.isLocked());
        assertEquals(null, defaultDomain.getLockOwner());
        assertEquals("/default-domain", defaultDomain.getPath());
        assertEquals("", defaultDomain.getVersionLabel());
        assertEquals("Domain", defaultDomain.getType());
        assertEquals("Domain", defaultDomain.getTitle());
        assertEquals("project", defaultDomain.getState());
        assertEquals("default", defaultDomain.getRepository());

        client.shutdown();
    }

    @Test
    public void shouldFetchVersionLabelAndLockInfo() throws Exception {
        client.setResponse(MediaType.APPLICATION_JSON, HttpResponses.DOC_LOCK_AND_VERSIONNED);

        Session session = client.getSession("Administrator", "Administrator");
        Document file = (Document) session.newRequest("Document.Fetch").set("value", "/default-domain").execute();
        assertEquals("8243123c-34d0-4e33-b4b3-290cef008db0", file.getId());
        assertEquals(6, file.getFacets().size());
        assertEquals("Downloadable", file.getFacets().getString(0));
        assertEquals("Commentable", file.getFacets().getString(1));
        assertEquals("Asset", file.getFacets().getString(2));
        assertEquals("Versionable", file.getFacets().getString(3));
        assertEquals("Publishable", file.getFacets().getString(4));
        assertEquals("HasRelatedText", file.getFacets().getString(5));
        assertEquals("Administrator:2013-05-16T17:58:26.618+02:00", file.getLock());
        assertEquals("2013-05-16T17:58:26.618+02:00", file.getLockCreated().toString());
        assertEquals(true, file.isLocked());
        assertEquals("Administrator", file.getLockOwner());
        assertEquals("/default-domain/UserWorkspaces/Administrator/My File", file.getPath());
        assertEquals("1.1", file.getVersionLabel());
        assertFalse(file.isCheckedOut());
        assertEquals("File", file.getType());
        assertEquals("My File", file.getTitle());
        assertEquals("project", file.getState());
        assertEquals("default", file.getRepository());

        client.shutdown();
    }
}
