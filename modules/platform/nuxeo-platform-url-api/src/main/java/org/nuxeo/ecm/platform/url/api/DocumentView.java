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
package org.nuxeo.ecm.platform.url.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * Document information describing a document context.
 * <p>
 * Some information is required (document location). Other information (like the currently selected tab) are handled
 * through parameters.*
 * <p>
 * This interface is used to map a url to a document context.
 */
public interface DocumentView {

    /**
     * Returns the url pattern names used to generate this document view.
     */
    String getPatternName();

    void setPatternName(String patternName);

    void setDocumentLocation(DocumentLocation documentLocation);

    DocumentLocation getDocumentLocation();

    Map<String, String> getParameters();

    String getParameter(String name);

    void addParameter(String name, String value);

    void removeParameter(String name);

    /**
     * Returns the outcome to use for this document view.
     * <p>
     * XXX AT: Can be considered to be badly named "view id".
     */
    String getViewId();

    void setViewId(String viewId);

    String getSubURI();

    void setSubURI(String subURI);

}
