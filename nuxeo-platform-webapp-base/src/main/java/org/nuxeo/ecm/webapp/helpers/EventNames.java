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
 * $Id$
 */

package org.nuxeo.ecm.webapp.helpers;

/**
 * Seam event identifiers.
 * <p>
 * This should stay with nuxeo, since it is related to the nuxeo infrastructure.
 * All code that depends on nuxeo infrastructure will need to depend on nuxeo
 * anyways. NXCommon is not a good place to move this because it is used from
 * nxruntime. It is not a good idea to add web client dependencies there.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public final class EventNames {
    /**
     * This is fired when the user selection changes. This should be listened by
     * componens that want to do some work when the user selection changes,
     * regardles of the type of selected document.
     */
    public static final String USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED = "userAllDocumentTypesSelectionChanged";

    /**
     * Fired when the selected domain changes. Should be listened by components
     * interested specifically in domain selection change.
     */
    public static final String DOMAIN_SELECTION_CHANGED = "domainSelectionChanged";

    /**
     * Fired when content root selection is changed (like workspaces root,
     * section root).
     */
    public static final String CONTENT_ROOT_SELECTION_CHANGED = "contentRootSelectionChanged";

    /**
     * Fired when a workspace root or section root selection changes.
     *
     * @deprecated content root children should be managed as regular documents
     */
    @Deprecated
    public static final String CONTENT_ROOT_CHILD_SELECTION_CHANGED = "contentRootChildSelectionChanged";

    /**
     * Fired when a document selection changes ( file, folder etc not workspace
     * or above ).
     */
    public static final String DOCUMENT_SELECTION_CHANGED = "documentSelectionChanged";

    /**
     * Fired when a folderish document selection changes.
     */
    public static final String FOLDERISHDOCUMENT_SELECTION_CHANGED = "folderishDocumentSelectionChanged";

    /**
     * Fired when a location selection changes.
     */
    public static final String LOCATION_SELECTION_CHANGED = "locationSelectionChanged";


    /**
     * Should be raised before an edited document is saved.
     * @since 5.4.2
     */
    public static final String BEFORE_DOCUMENT_CHANGED = "beforeDocumentChanged";

    /**
     * Should be raised when a document is edited.
     */
    public static final String DOCUMENT_CHANGED = "documentChanged";

    /**
     * Should be raised when a stateful QueryModel is edited.
     */
    public static final String QUERY_MODEL_CHANGED = "queryModelChanged";

    /**
     * Should be raised when the children of the current document are modified.
     */
    public static final String DOCUMENT_CHILDREN_CHANGED = "documentChildrenChanged";

    /**
     * Should be raised when the user goes Home. This is useful to allow any
     * components to revert to their uninitialized state if needed.
     * <p>
     * Ex: The tree and breadcrumb should not display anything because home is
     * view_servers and they display what's inside a domain.
     */
    public static final String GO_HOME = "goHome";

    /**
     * Will be raised when a changeable document is created (but not saved).
     */
    public static final String NEW_DOCUMENT_CREATED = "changeableDocumentCreated";

    /**
     * Will be raised when a document is submitted for publication.
     */
    public static final String DOCUMENT_SUBMITED_FOR_PUBLICATION = "documentSubmitedForPublication";

    /**
     * This is raised when a proxy is created and need a moderation
     */
    public static final String PROXY_PUSLISHING_PENDING = "proxyPublishingPending";

    public static final String DOCUMENT_UNPUBLISHED = "documentUnPublished";

    /**
     * This is raised when a proxy has been published.
     */
    public static final String PROXY_PUBLISHED = "proxyPublished";

    /**
     * This is raised when a document is published
     */
    public static final String DOCUMENT_PUBLISHED = "documentPublished";

    /**
     * This is raised when a document publication is rejected
     */
    public static final String DOCUMENT_PUBLICATION_REJECTED = "documentPublicationRejected";

    /**
     * This is raised when a document publication is rejected
     */
    public static final String DOCUMENT_PUBLICATION_APPROVED = "documentPublicationApproved";

    /**
     * Event raised when security is changed
     */
    public static final String DOCUMENT_SECURITY_CHANGED = "documentSecurityChanged";

    /**
     * Event raised when a search is performed
     */
    public static final String SEARCH_PERFORMED = "searchPerformed";

    /**
     * Event raised when user go to his personal workspace
     */
    public static final String GO_PERSONAL_WORKSPACE = "personnalWorkspace";

    /**
     * Event raised when the local configuration of a document has changed
     */
    public static final String LOCAL_CONFIGURATION_CHANGED = "localConfigurationChanged";

    // Constant utility class.
    private EventNames() {
    }

}
