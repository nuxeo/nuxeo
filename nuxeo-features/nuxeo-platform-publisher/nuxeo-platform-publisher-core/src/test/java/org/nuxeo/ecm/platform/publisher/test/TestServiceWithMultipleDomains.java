/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@LocalDeploy("org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-content-template-contrib.xml")
public class TestServiceWithMultipleDomains extends PublisherTestCase {

    protected DocumentModel doc2Publish;

    protected void createInitialDocs(String domainPath) throws Exception {
        DocumentModel wsRoot = session.getDocument(new PathRef(
                domainPath + "/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        DocumentModel sectionsRoot = session.getDocument(new PathRef(
                domainPath + "/sections"));

        DocumentModel section1 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);

        DocumentModel section2 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section2", "Section");
        section2.setProperty("dublincore", "title", "section2");
        section2 = session.createDocument(section2);

        DocumentModel section11 = session.createDocumentModel(
                section1.getPathAsString(), "section11", "Section");
        section11.setProperty("dublincore", "title", "section11");
        section11 = session.createDocument(section11);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testTreeRegistration() throws Exception {
        createInitialDocs("default-domain");
        createInitialDocs("another-default-domain");

        PublisherService service = Framework.getLocalService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();
        assertEquals(2, treeNames.size());
        assertTrue(treeNames.contains("DefaultSectionsTree-default-domain"));
        assertTrue(treeNames.contains("DefaultSectionsTree-another-default-domain"));
    }

}
