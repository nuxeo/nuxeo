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
     * Provides an explaining message of the inc options availability.
     *
     * @return key for the message (to be read from messages bundles)
     *
     * @deprecated since 5.4
     */
    @Deprecated
    String getIncRulesResult();

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
     * Returns the available versioning options for the currentItem and state.
     *
     * @return a collection of option names.
     *
     * @deprecated since 5.4
     */
    Collection<VersioningActions> getCurrentItemVersioningOptions();

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
     *
     * @deprecated since 5.4
     */
    @Deprecated
    Map<String, String> getVersioningOptionsMap(
            final DocumentModel documentModel);

    Map<String, String> getAvailableVersioningOptionsMap();

    String getVersionLabel(DocumentModel document) throws ClientException;

    String getVersioningOptionInstanceId();

    String factoryForIncrementationRules();

    boolean factoryForRenderVersioningOption();

    /**
     * Web action method to set version increment option to the current
     * documentModel.
     */
    void setVersioningOptionInstanceId(String optionId) throws ClientException;

    void setVersioningOptionInstanceId(DocumentModel document, String optionId)
            throws ClientException;

    void setVersioningOptionInstanceId(DocumentModel document,
            VersioningActions option) throws ClientException;

    /**
     * Versioning increment options - select radio component validator method.
     * Check if an option has been selected. This is mandatory since the
     * component is being displayed.
     */
    void validateOptionSelection(FacesContext context, UIComponent component,
            Object value);

    /**
     * @deprecated since 5.4
     */
    @Deprecated
    void setCreateSnapshot(boolean createSnapshot);

    /**
     * @deprecated since 5.4
     */
    @Deprecated
    boolean getCreateSnapshot() throws ClientException;

    /**
     * @deprecated since 5.4
     */
    @Deprecated
    boolean getDefaultCreateSnapshot() throws ClientException;

    /**
     * @deprecated since 5.4
     */
    @Deprecated
    boolean getDisplayCreateSnapshotOption() throws ClientException;
}
