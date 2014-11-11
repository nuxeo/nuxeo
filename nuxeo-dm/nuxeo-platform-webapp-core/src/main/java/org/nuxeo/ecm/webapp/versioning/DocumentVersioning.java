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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.versioning;

import java.util.Collection;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Web action listener interface for versioning.
 *
 * @author Dragos Mihalache
 */
public interface DocumentVersioning {

    /**
     * Returns the available versioning options for the document parameter and
     * state.
     *
     * @param document the document for which the versioning options will be
     *            returned
     * @return a collection of option names.
     */
    Collection<VersionModel> getItemVersioningHistory(DocumentModel document);

    /**
     * Returns the available versioning history for the current document and
     * state.
     *
     * @return a collection of option names.
     */
    Collection<VersionModel> getCurrentItemVersioningHistory();

    /**
     * Creates a Map with versioning options (as keys) and labels (as map entry
     * values).
     */
    Map<String, String> getVersioningOptionsMap(
            final DocumentModel documentModel);

    /**
     * @deprecated since 5.7.3: available versioning options are resolved by
     *             the widget now
     */
    @Deprecated
    Map<String, String> getAvailableVersioningOptionsMap();

    String getVersionLabel(DocumentModel document) throws ClientException;

    /**
     * @deprecated since 5.7.3: selected option is not kept on this bean
     *             anymore, it's kept by the JSF component behind widget
     *             definition
     */
    @Deprecated
    String getVersioningOptionInstanceId();

    /**
     * @deprecated since 5.7.3: rendered clause for available versioning
     *             options are resolved by the widget now
     */
    @Deprecated
    boolean factoryForRenderVersioningOption();

    /**
     * Web action method to set version increment option to the current
     * documentModel.
     *
     * @deprecated since 5.7.3: document context map is now filled directly by
     *             the widget
     */
    @Deprecated
    void setVersioningOptionInstanceId(String optionId) throws ClientException;

    /**
     * @deprecated since 5.7.3: document context map is now filled directly by
     *             the widget
     */
    @Deprecated
    void setVersioningOptionInstanceId(DocumentModel document, String optionId)
            throws ClientException;

    /**
     * @deprecated since 5.7.3: document context map is now filled directly by
     *             the widget
     */
    @Deprecated
    void setVersioningOptionInstanceId(DocumentModel document,
            VersioningActions option) throws ClientException;

    /**
     * Versioning increment options - select radio component validator method.
     * Check if an option has been selected. This is mandatory since the
     * component is being displayed.
     */
    void validateOptionSelection(FacesContext context, UIComponent component,
            Object value);

}
