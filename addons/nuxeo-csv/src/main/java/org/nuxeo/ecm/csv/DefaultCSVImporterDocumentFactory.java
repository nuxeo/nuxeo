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

import static org.nuxeo.ecm.core.api.LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class DefaultCSVImporterDocumentFactory implements CSVImporterDocumentFactory {

    private static final long serialVersionUID = 1L;

    public static final List<String> IGNORE_FIELDS_ON_UPDATE = Arrays.asList(NXQL.ECM_LIFECYCLESTATE);

    @Override
    public void createDocument(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values) {
        DocumentModel doc = session.createDocumentModel(parentPath, name, type);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            if (NXQL.ECM_LIFECYCLESTATE.equals(entry.getKey())) {
                doc.putContextData(INITIAL_LIFECYCLE_STATE_OPTION_NAME, entry.getValue());
            } else {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        }
        session.createDocument(doc);
    }

    @Override
    public void updateDocument(CoreSession session, DocumentRef docRef, Map<String, Serializable> values)
            {
        DocumentModel doc = session.getDocument(docRef);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            if (!IGNORE_FIELDS_ON_UPDATE.contains(entry.getKey())) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        }
        session.saveDocument(doc);
    }

    @Override
    public boolean exists(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values) {
        String targetPath = new Path(parentPath).append(name).toString();
        DocumentRef docRef = new PathRef(targetPath);
        return session.exists(docRef);
    }
}
