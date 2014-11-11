/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentViewImpl.java 28460 2008-01-03 15:34:05Z sfermigier $
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

    public DocumentViewImpl(DocumentLocation documentLocation, String viewId,
            Map<String, String> parameters) {
        this.documentLocation = documentLocation;
        this.viewId = viewId;
        this.parameters = parameters;
    }

    // Not used. Deprecate? please keep: this is the most complete constructor.
    public DocumentViewImpl(DocumentLocation documentLocation, String viewId,
            String subURI, Map<String, String> parameters) {
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

    @Deprecated
    public DocumentViewImpl(DocumentLocation docLoc, String viewId,
            String tabId, String subURI) {
        documentLocation = docLoc;
        this.viewId = viewId;
        this.tabId = tabId;
        if (subURI != null) {
            subURI = subURI.trim();
            if (subURI.length() == 0) {
                subURI = null;
            }
        }
        this.subURI = subURI;
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
            parameters = new HashMap<String, String>();
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
            parameters = new HashMap<String, String>();
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
                documentLocation, parameters, patternName, subURI, tabId,
                viewId);
    }

}
