/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Deploy("org.nuxeo.ecm.platform.publisher.test:OSGI-INF/publisher-content-template-contrib.xml")
public class TestServiceWithMultipleDomains extends PublisherTestCase {

    @Inject
    protected PublisherService publisherService;

    @Inject
    protected TrashService trashService;

    protected void createInitialDocs(String domainPath) {
        DocumentModel wsRoot = session.getDocument(new PathRef(domainPath + "/workspaces"));

        DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        DocumentModel sectionsRoot = session.getDocument(new PathRef(domainPath + "/sections"));

        DocumentModel section1 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);

        DocumentModel section2 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section2", "Section");
        section2.setProperty("dublincore", "title", "section2");
        section2 = session.createDocument(section2);

        DocumentModel section11 = session.createDocumentModel(section1.getPathAsString(), "section11", "Section");
        section11.setProperty("dublincore", "title", "section11");
        section11 = session.createDocument(section11);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testTreeRegistration() {
        createInitialDocs("/default-domain");
        createInitialDocs("/another-default-domain");

        List<String> treeNames = publisherService.getAvailablePublicationTree();
        assertEquals(2, treeNames.size());
        assertTrue(treeNames.contains("DefaultSectionsTree-default-domain"));
        assertTrue(treeNames.contains("DefaultSectionsTree-another-default-domain"));
    }

    @Test
    public void testTreeRegistrationWhenTrashingDomain() {
        List<String> treeNames = publisherService.getAvailablePublicationTree();
        assertEquals(2, treeNames.size());
        assertTrue(treeNames.contains("DefaultSectionsTree-default-domain"));
        assertTrue(treeNames.contains("DefaultSectionsTree-another-default-domain"));

        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        trashService.trashDocument(domain);

        treeNames = publisherService.getAvailablePublicationTree();
        assertEquals(1, treeNames.size());
    }

}
