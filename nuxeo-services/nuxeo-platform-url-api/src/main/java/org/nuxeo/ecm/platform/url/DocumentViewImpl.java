/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.url;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * TODO: document me.
 *
 * @author tiry
 */
public class DocumentViewImpl implements DocumentView, Serializable {

    private static final long serialVersionUID = 1L;

    private DocumentLocation documentLocation;

    private String viewId;

    private String tabId;

    private String subURI;

    private String patternName;

    private Map<String, String> parameters;

    public DocumentViewImpl(DocumentLocation documentLocation, String viewId) {
        this.documentLocation = documentLocation;
        this.viewId = viewId;
    }

    public DocumentViewImpl(DocumentLocation documentLocation, String viewId, Map<String, String> parameters) {
        this.documentLocation = documentLocation;
        this.viewId = viewId;
        this.parameters = parameters;
    }

    // Not used. Deprecate? please keep: this is the most complete constructor.
    public DocumentViewImpl(DocumentLocation documentLocation, String viewId, String subURI,
            Map<String, String> parameters) {
        this.documentLocation = documentLocation;
        this.viewId = viewId;
        this.subURI = subURI;
        this.parameters = parameters;
    }

    public DocumentViewImpl(DocumentModel doc) {
        documentLocation = new DocumentLocationImpl(doc);
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        viewId = typeInfo.getDefaultView();
    }

    public DocumentViewImpl(DocumentLocation docLoc) {
        documentLocation = docLoc;
        subURI = null;
    }

    public DocumentLocation getDocumentLocation() {
        return documentLocation;
    }

    public String getTabId() {
        if (tabId == null && parameters != null) {
            return parameters.get("tabId");
        }
        return tabId;
    }

    public String getViewId() {
        if (viewId == null && parameters != null) {
            return parameters.get("viewId");
        }
        return viewId;
    }

    public String getSubURI() {
        return subURI;
    }

    public Map<String, String> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        String tabId = getTabId();
        if (tabId != null) {
            parameters.put("tabId", tabId);
        }
        return Collections.unmodifiableMap(parameters);
    }

    public String getParameter(String name) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(name);
    }

    public void addParameter(String name, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        if (parameters == null) {
            return;
        }
        parameters.remove(name);
    }

    public void setDocumentLocation(DocumentLocation documentLocation) {
        this.documentLocation = documentLocation;
    }

    public void setSubURI(String subURI) {
        this.subURI = subURI;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    @Override
    public String toString() {
        return String.format(
                "DocumentViewImpl [documentLocation=%s, "
                        + "parameters=%s, patternName=%s, subURI=%s, tabId=%s, viewId=%s]",
                documentLocation, parameters, patternName, subURI, tabId, viewId);
    }

}
