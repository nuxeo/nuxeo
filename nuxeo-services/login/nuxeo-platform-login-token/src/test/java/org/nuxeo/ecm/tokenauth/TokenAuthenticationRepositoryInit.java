/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Initializes the repository for token authentication test cases. Only create one document for now.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationRepositoryInit extends DefaultRepositoryInit {

    public static String getTestDocPath() {
        return "/testDoc";
    }

    @Override
    public void populate(CoreSession session) {
        createTestDoc(session);
    }

    /**
     * Creates the test doc.
     */
    protected DocumentModel createTestDoc(CoreSession session) {

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");

        doc.setPropertyValue("dc:title", "My test doc");
        doc.setPropertyValue("dc:description", "For test purpose.");

        return session.createDocument(doc);
    }

}
