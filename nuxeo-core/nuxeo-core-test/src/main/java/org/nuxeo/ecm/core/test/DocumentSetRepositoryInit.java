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
package org.nuxeo.ecm.core.test;

import java.util.Date;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * @since 10.2
 */
public class DocumentSetRepositoryInit extends DefaultRepositoryInit {

    public static final String USERNAME = "bob";

    public static final String ROOT = "/default-domain/workspaces/test";

    public static final int SIZE = 3;

    public static final int DOC_BY_SIZE = 5;

    public static final int DOC_BY_LEVEL = SIZE * DOC_BY_SIZE;

    public static final int CREATED_NON_PROXY;

    public static final int CREATED_PROXY;

    public static final int CREATED_TOTAL;

    static {
        // with SIZE = 3 AND DOC_BY_SIZE = 5, 195 documents are created
        int total = 0;
        for (int i = 1; i <= SIZE; i++) {
            total += DOC_BY_SIZE * Math.pow(SIZE, i);
        }
        CREATED_TOTAL = total;
        CREATED_PROXY = total / DOC_BY_SIZE;
        CREATED_NON_PROXY = total / DOC_BY_SIZE * (DOC_BY_SIZE - 1);
    }

    @Override
    public void populate(CoreSession session) {
        super.populate(session);
        DocumentModel test = session.getDocument(new PathRef(ROOT));
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acl.add(new ACE(USERNAME, "WriteProperties", true));
        acl.add(new ACE(USERNAME, "Read", true));
        acp.addACL(acl);
        test.setACP(acp, false);
        createChildren(session, test, SIZE);
    }

    private void createChildren(CoreSession s, DocumentModel p, int depth) {
        if (depth > 0) {
            for (int i = 0; i < SIZE; i++) {
                s.createDocument(s.createDocumentModel(p.getPathAsString(), p.getName() + "file" + i, "File"));
                s.createDocument(s.createDocumentModel(p.getPathAsString(), p.getName() + "note" + i, "Note"));
                DocumentModel child = s.createDocumentModel(p.getPathAsString(), p.getName() + "doc" + i, "ComplexDoc");
                child.setPropertyValue("dc:modified", new Date());
                child.setPropertyValue("dc:nature", "article");
                child.setPropertyValue("dc:contributors", new String[] { "bob", "Administrator" });
                child = s.createDocument(child);
                s.createProxy(child.getRef(), p.getRef());
                DocumentModel folder = s.createDocumentModel(p.getPathAsString(), p.getName() + "folder" + i, "Folder");
                folder = s.createDocument(folder);
                createChildren(s, folder, depth - 1);
            }
        }
    }
}
