/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.versioning;

import java.util.Collection;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;

/**
 * Web action listener interface for versioning.
 *
 * @author Dragos Mihalache
 */
public interface DocumentVersioning {

    /**
     * Returns the available versioning options for the document parameter and state.
     *
     * @param document the document for which the versioning options will be returned
     * @return a collection of option names.
     */
    Collection<VersionModel> getItemVersioningHistory(DocumentModel document);

    /**
     * Returns the available versioning history for the current document and state.
     *
     * @return a collection of option names.
     */
    Collection<VersionModel> getCurrentItemVersioningHistory();

    /**
     * Creates a Map with versioning options (as keys) and labels (as map entry values).
     */
    Map<String, String> getVersioningOptionsMap(final DocumentModel documentModel);

    String getVersionLabel(DocumentModel document);

    /**
     * Versioning increment options - select radio component validator method. Check if an option has been selected.
     * This is mandatory since the component is being displayed.
     */
    void validateOptionSelection(FacesContext context, UIComponent component, Object value);

}
