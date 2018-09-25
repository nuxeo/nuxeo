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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @since 10.2
 */
public class DocumentSetRepositoryInit extends DefaultRepositoryInit {

    public static final String ROOT = "/default-domain/workspaces/test";

    public static final int SIZE = 3;

    public static final int DOC_BY_LEVEL = SIZE * 2;

    public static int created_non_proxy;

    public static int created_proxy;

    public static int created_total;

    static {
        // with SIZE = 3, 9 + 27 + 81 = 117 documents are created
        for (int i = 1; i <= SIZE; i++) {
            created_total += 3 * Math.pow(SIZE, i);
        }
        created_proxy = created_total / 3;
        created_non_proxy = created_total / 3 * 2;
    }

    @Override
    public void populate(CoreSession session) {
        super.populate(session);
        DocumentModel test = session.getDocument(new PathRef(ROOT));
        createChildren(session, test, SIZE);
    }

    private void createChildren(CoreSession s, DocumentModel p, int depth) {
        if (depth > 0) {
            for (int i = 0; i < SIZE; i++) {
                DocumentModel child = s.createDocumentModel(p.getPathAsString(), p.getName() + "doc" + i, "ComplexDoc");
                child = s.createDocument(child);
                s.saveDocument(child);
                s.createProxy(child.getRef(), p.getRef());
                s.saveDocument(child);
                DocumentModel folder = s.createDocumentModel(p.getPathAsString(), p.getName() + "folder" + i, "Folder");
                folder = s.createDocument(folder);
                s.saveDocument(folder);
                createChildren(s, folder, depth - 1);
            }
        }
    }
}
