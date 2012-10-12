/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Initializes the repository for token authentication test cases. Only create
 * one document for now.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationRepositoryInit extends DefaultRepositoryInit {

    public static String getTestDocPath() {
        return "/testDoc";
    }

    @Override
    public void populate(CoreSession session) throws ClientException {
        createTestDoc(session);
    }

    /**
     * Creates the test doc.
     */
    protected DocumentModel createTestDoc(CoreSession session)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");

        doc.setPropertyValue("dc:title", "My test doc");
        doc.setPropertyValue("dc:description", "For test purpose.");

        return session.createDocument(doc);
    }

}
