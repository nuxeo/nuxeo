/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import org.nuxeo.ecm.automation.test.adapters.BusinessBeanAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * Repo init to test Rest API
 *
 *
 * @since 5.7.2
 */
public class RestServerInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) throws ClientException {
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "folder_" + i,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder " + i);
            doc = session.createDocument(doc);
        }

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/folder_1",
                    "note_" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);

            doc.getAdapter(BusinessBeanAdapter.class).setNote("Note " + i);
            doc = session.createDocument(doc);
        }

        session.save();

    }

    public static DocumentModel getFolder(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_" + index));
    }

    public static DocumentModel getNote(int index, CoreSession session)
            throws ClientException {
        return session.getDocument(new PathRef("/folder_1/note_" + index));
    }

}
