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
 * $Id: DocumentView.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * Document information describing a document context.
 * <p>
 * Some information is required (document location). Other information (like the
 * currently selected tab) are handled through parameters.*
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

    /**
     * @deprecated should use the parameters map for the tab identifier.
     */
    @Deprecated
    String getTabId();

}
