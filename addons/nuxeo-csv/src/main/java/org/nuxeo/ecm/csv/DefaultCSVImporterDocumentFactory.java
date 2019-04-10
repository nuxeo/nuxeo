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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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

    public static final String DC_CREATOR = "dc:creator";

    public static final String DC_CONTRIBUTORS = "dc:contributors";

    public static final List<String> IGNORE_FIELDS_ON_UPDATE = Arrays.asList(NXQL.ECM_LIFECYCLESTATE);

    @Override
    public void createDocument(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values) {
        values = prepareValues(values);
        DocumentModel doc = session.createDocumentModel(parentPath, name, type);
        if (values.containsKey(NXQL.ECM_LIFECYCLESTATE)) {
            doc.putContextData(INITIAL_LIFECYCLE_STATE_OPTION_NAME, values.get(NXQL.ECM_LIFECYCLESTATE));
            values.remove(NXQL.ECM_LIFECYCLESTATE);
        }
        doc = session.createDocument(doc);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }
        session.saveDocument(doc);
    }

    protected Map<String, Serializable> prepareValues(Map<String, Serializable> values) {
        if (values.containsKey(DC_CREATOR)) {
            // make sure the creator is part of the contributors
            String creator = (String) values.get(DC_CREATOR);
            String[] contributorsArray = (String[]) values.get(DC_CONTRIBUTORS);
            List<String> contributors = contributorsArray == null ? new ArrayList<>() : new ArrayList<>(
                    Arrays.asList(contributorsArray));
            if (StringUtils.isNotBlank(creator) && !contributors.contains(creator)) {
                contributors.add(creator);
            }
            values.put(DC_CONTRIBUTORS, contributors.toArray(new String[contributors.size()]));
        }
        return values;
    }

    @Override
    public void updateDocument(CoreSession session, DocumentRef docRef, Map<String, Serializable> values) {
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
