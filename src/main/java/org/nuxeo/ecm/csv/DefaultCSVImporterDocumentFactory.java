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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class DefaultCSVImporterDocumentFactory implements
        CSVImporterDocumentFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public void createDocument(CoreSession session, String parentPath,
            String name, String type, Map<String, Serializable> values)
            throws ClientException {
        DocumentModel doc = session.createDocumentModel(parentPath, name, type);
        session.createDocument(doc);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }
        session.saveDocument(doc);
    }

    @Override
    public void updateDocument(CoreSession session, DocumentRef docRef,
            Map<String, Serializable> values) throws ClientException {
        DocumentModel doc = session.getDocument(docRef);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }
        session.saveDocument(doc);
    }

    @Override
    public boolean exists(CoreSession session, String parentPath, String name,
            String type, Map<String, Serializable> values)
            throws ClientException {
        String targetPath = new Path(parentPath).append(name).toString();
        DocumentRef docRef = new PathRef(targetPath);
        return session.exists(docRef);
    }
}