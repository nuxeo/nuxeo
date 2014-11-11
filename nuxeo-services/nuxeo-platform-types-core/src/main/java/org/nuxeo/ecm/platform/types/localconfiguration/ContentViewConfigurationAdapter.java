/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code ContentViewConfiguration}.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public class ContentViewConfigurationAdapter extends
        AbstractLocalConfiguration<ContentViewConfiguration> implements
        ContentViewConfiguration {

    private static final Log log = LogFactory.getLog(ContentViewConfigurationAdapter.class);

    protected DocumentRef documentRef;

    protected Map<String, List<String>> typeToContentViewNames;

    protected boolean canMerge = true;

    @SuppressWarnings("unchecked")
    public ContentViewConfigurationAdapter(DocumentModel doc) {
        documentRef = doc.getRef();
        typeToContentViewNames = new HashMap<String, List<String>>();
        try {
            List<Map<String, String>> cvNamesByType = (List<Map<String, String>>) doc.getPropertyValue(CONTENT_VIEW_CONFIGURATION_NAMES_BY_TYPE);
            for (Map<String, String> typeToCv : cvNamesByType) {
                String type = typeToCv.get(CONTENT_VIEW_CONFIGURATION_DOC_TYPE);
                String cvName = typeToCv.get(CONTENT_VIEW_CONFIGURATION_CONTENT_VIEW);
                if (typeToContentViewNames.containsKey(type)) {
                    typeToContentViewNames.get(type).add(cvName);
                } else {
                    List<String> cvNames = new ArrayList<String>();
                    cvNames.add(cvName);
                    typeToContentViewNames.put(type, cvNames);
                }
            }
        } catch (ClientException e) {
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
