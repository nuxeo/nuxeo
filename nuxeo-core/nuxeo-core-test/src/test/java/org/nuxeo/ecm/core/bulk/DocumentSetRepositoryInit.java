/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import java.util.Date;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @since 10.2
 */
public class DocumentSetRepositoryInit extends DefaultRepositoryInit {

    private static final int SIZE = 10;

    @Override
    public void populate(CoreSession session) {
        super.populate(session);
        DocumentModel test = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        for (int i = 0; i < SIZE; i++) {
            DocumentModel doc = session.createDocumentModel(test.getPathAsString(), "doc" + i, "ComplexDoc");
            doc.setProperty("dublincore", "modified", new Date());
            session.createDocument(doc);
        }
    }
}
