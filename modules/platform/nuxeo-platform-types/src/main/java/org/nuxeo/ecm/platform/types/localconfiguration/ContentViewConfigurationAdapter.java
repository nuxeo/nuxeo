/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_CONTENT_VIEW;
import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_NAMES_BY_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code ContentViewConfiguration}.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public class ContentViewConfigurationAdapter extends AbstractLocalConfiguration<ContentViewConfiguration> implements
        ContentViewConfiguration {

    private static final Log log = LogFactory.getLog(ContentViewConfigurationAdapter.class);

    protected DocumentRef documentRef;

    protected Map<String, List<String>> typeToContentViewNames;

    protected boolean canMerge = true;

    @SuppressWarnings("unchecked")
    public ContentViewConfigurationAdapter(DocumentModel doc) {
        documentRef = doc.getRef();
        typeToContentViewNames = new HashMap<>();
        try {
            List<Map<String, String>> cvNamesByType = (List<Map<String, String>>) doc.getPropertyValue(CONTENT_VIEW_CONFIGURATION_NAMES_BY_TYPE);
            for (Map<String, String> typeToCv : cvNamesByType) {
                String type = typeToCv.get(CONTENT_VIEW_CONFIGURATION_DOC_TYPE);
                String cvName = typeToCv.get(CONTENT_VIEW_CONFIGURATION_CONTENT_VIEW);
                if (typeToContentViewNames.containsKey(type)) {
                    typeToContentViewNames.get(type).add(cvName);
                } else {
                    List<String> cvNames = new ArrayList<>();
                    cvNames.add(cvName);
                    typeToContentViewNames.put(type, cvNames);
                }
            }
        } catch (PropertyException e) {
            log.error("Failed to get ContentViewConfiguration", e);
        }
    }

    @Override
    public List<String> getContentViewsForType(String docType) {
        return typeToContentViewNames.get(docType);
    }

    @Override
    public boolean canMerge() {
        return canMerge;
    }

    @Override
    public DocumentRef getDocumentRef() {
        return documentRef;
    }

    @Override
    public ContentViewConfiguration merge(ContentViewConfiguration other) {
        if (other == null) {
            return this;
        }

        // set the documentRef to the other UITypesConfiguration to continue
        // merging, if needed
        documentRef = other.getDocumentRef();

        Map<String, List<String>> t2cv = other.getTypeToContentViewNames();
        for (String type : t2cv.keySet()) {
            if (!typeToContentViewNames.containsKey(type)) {
                typeToContentViewNames.put(type, t2cv.get(type));
            }
        }

        return this;
    }

    @Override
    public Map<String, List<String>> getTypeToContentViewNames() {
        return typeToContentViewNames;
    }

}
