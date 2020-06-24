/*
 * (C) Copyright 2011-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

/**
 * Adapter from a Nuxeo document to the {@link OperationInfo} interface.
 */
public class OperationInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements OperationInfo {

    protected OperationInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    // artifact id with type-specific prefix
    @Override
    public String getId() {
        return ARTIFACT_PREFIX + getName();
    }

    @Override
    public String getName() {
        return safeGet(PROP_NAME);
    }

    @Override
    public List<String> getAliases() {
        return safeGet(PROP_ALIASES);
    }

    @Override
    public String getVersion() {
        return safeGet(PROP_VERSION);
    }

    @Override
    public String getDescription() {
        return safeGet(PROP_DESCRIPTION);
    }

    @Override
    public List<String> getSignature() {
        return safeGet(PROP_SIGNATURE);
    }

    @Override
    public String getCategory() {
        return safeGet(PROP_CATEGORY);
    }

    @Override
    public String getUrl() {
        return safeGet(PROP_URL);
    }

    @Override
    public String getLabel() {
        return safeGet(PROP_LABEL);
    }

    @Override
    public String getRequires() {
        return safeGet(PROP_REQUIRES);
    }

    @Override
    public String getSince() {
        return safeGet(PROP_SINCE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Param> getParams() {
        List<Map<String, Serializable>> maps = (List<Map<String, Serializable>>) doc.getPropertyValue(PROP_PARAMS);
        List<Param> params = new ArrayList<>();
        if (maps != null) {
            for (Map<String, Serializable> map : maps) {
                Param p = new Param();
                p.name = (String) map.get(PROP_PARAM_NAME);
                p.type = (String) map.get(PROP_PARAM_TYPE);
                p.widget = (String) map.get(PROP_PARAM_WIDGET);
                p.values = ((List<String>) map.get(PROP_PARAM_VALUES)).toArray(new String[0]);
                Long order = (Long) map.get(PROP_PARAM_ORDER);
                p.order = order == null ? 0 : order.intValue();
                Boolean required = (Boolean) map.get(PROP_PARAM_REQUIRED);
                p.required = required == null ? false : required.booleanValue();
                params.add(p);
            }
        }
        return params;
    }

    @Override
    public int compareTo(OperationInfo o) {
        String s1 = getLabel() == null ? getId() : getLabel();
        String s2 = o.getLabel() == null ? o.getId() : o.getLabel();
        return s1.compareTo(s2);
    }

    /**
     * Creates an actual document from the {@link OperationInfo}.
     */
    public static OperationInfo create(OperationInfo oi, CoreSession session, String containerPath) {
        String name = computeDocumentName(oi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exists = session.exists(new PathRef(targetPath));
        DocumentModel doc;
        if (exists) {
            doc = session.getDocument(new PathRef(targetPath));
        } else {
            doc = session.createDocumentModel(TYPE_NAME);
            doc.setPathInfo(containerPath, name);
        }
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, oi.getName());
        doc.setPropertyValue(PROP_NAME, oi.getName());
        doc.setPropertyValue(PROP_ALIASES, (Serializable) oi.getAliases());
        doc.setPropertyValue(PROP_VERSION, oi.getVersion());
        doc.setPropertyValue(PROP_DESCRIPTION, oi.getDescription());
        doc.setPropertyValue(PROP_SIGNATURE, (Serializable) oi.getSignature());
        doc.setPropertyValue(PROP_CATEGORY, oi.getCategory());
        doc.setPropertyValue(PROP_URL, oi.getUrl());
        doc.setPropertyValue(PROP_LABEL, oi.getLabel());
        doc.setPropertyValue(PROP_REQUIRES, oi.getRequires());
        doc.setPropertyValue(PROP_SINCE, oi.getSince());
        doc.setPropertyValue(PROP_OP_CLASS, oi.getOperationClass());
        doc.setPropertyValue(PROP_CONTRIBUTING_COMPONENT, oi.getContributingComponent());
        List<Map<String, Serializable>> params = new ArrayList<>();
        for (Param p : oi.getParams()) {
            Map<String, Serializable> map = new HashMap<>();
            map.put(PROP_PARAM_NAME, p.getName());
            map.put(PROP_PARAM_TYPE, p.getType());
            map.put(PROP_PARAM_WIDGET, p.getWidget());
            map.put(PROP_PARAM_VALUES, p.getValues());
            map.put(PROP_PARAM_REQUIRED, Boolean.valueOf(p.isRequired()));
            map.put(PROP_PARAM_ORDER, Long.valueOf(p.getOrder()));
            params.add(map);
        }
        doc.setPropertyValue(PROP_PARAMS, (Serializable) params);
        doc.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true);
        if (exists) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new OperationInfoDocAdapter(doc);
    }

    @Override
    public String getOperationClass() {
        return safeGet(PROP_OP_CLASS);
    }

    @Override
    public String getContributingComponent() {
        return safeGet(PROP_CONTRIBUTING_COMPONENT);
    }

}
