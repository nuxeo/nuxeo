/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class DefaultCSVImporterDocumentFactory implements CSVImporterDocumentFactory {

    private static final long serialVersionUID = 1L;

    public static final String DC_CREATOR = "dc:creator";

    public static final String DC_CONTRIBUTORS = "dc:contributors";

    public static final List<String> IGNORE_FIELDS_ON_UPDATE = Collections.singletonList(NXQL.ECM_LIFECYCLESTATE);

    protected CSVImporterOptions importerOptions = CSVImporterOptions.DEFAULT_OPTIONS;

    @Override
    public void createDocument(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values) {
        values = prepareValues(values);
        DocumentModel doc = session.createDocumentModel(parentPath, name, type);
        if (values.containsKey(NXQL.ECM_LIFECYCLESTATE)) {
            doc.putContextData(INITIAL_LIFECYCLE_STATE_OPTION_NAME, values.get(NXQL.ECM_LIFECYCLESTATE));
            values.remove(NXQL.ECM_LIFECYCLESTATE);
        }
        if (importerOptions.importMode.equals(ImportMode.IMPORT)) {
            if (values.containsKey(NXQL.ECM_UUID)) {
                ((DocumentModelImpl) doc).setId((String) values.get(NXQL.ECM_UUID));
                values.remove(NXQL.ECM_UUID);
            } else {
                ((DocumentModelImpl) doc).setId(UUID.randomUUID().toString());
            }
            for (Map.Entry<String, Serializable> entry : values.entrySet()) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
            session.importDocuments(Collections.singletonList(doc));
        } else {
            if (values.containsKey(NXQL.ECM_UUID)) {
                throw new NuxeoException("CSV file contains UUID. Import using Import Mode to avoid overwriting.");
            }
            for (Map.Entry<String, Serializable> entry : values.entrySet()) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
            session.createDocument(doc);
        }
    }

    protected Map<String, Serializable> prepareValues(Map<String, Serializable> values) {
        if (values.containsKey(DC_CREATOR)) {
            // make sure the creator is part of the contributors
            String creator = (String) values.get(DC_CREATOR);
            String[] contributorsArray = (String[]) values.get(DC_CONTRIBUTORS);
            List<String> contributors = contributorsArray == null ? new ArrayList<>()
                    : new ArrayList<>(Arrays.asList(contributorsArray));
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
    public boolean exists(CoreSession session, String parentPath, String name, Map<String, Serializable> values) {
        String targetPath = new Path(parentPath).append(name).toString();
        DocumentRef docRef = new PathRef(targetPath);
        return session.exists(docRef);
    }

    @Override
    @Deprecated
    public boolean exists(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values) {
        return exists(session, parentPath, name, null);
    }

    @Override
    public void setImporterOptions(CSVImporterOptions importerOptions) {
        this.importerOptions = importerOptions;
    }

}
