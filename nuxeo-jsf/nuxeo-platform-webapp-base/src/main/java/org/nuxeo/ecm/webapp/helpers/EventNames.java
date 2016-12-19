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
package org.nuxeo.ecm.webapp.helpers;

import org.nuxeo.runtime.api.Framework;

/**
 * Seam event identifiers.
 * <p>
 * This should stay with nuxeo, since it is related to the nuxeo infrastructure. All code that depends on nuxeo
 * infrastructure will need to depend on nuxeo anyways. NXCommon is not a good place to move this because it is used
 * from nxruntime. It is not a good idea to add web client dependencies there.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public final class EventNames {
    /**
     * This is fired when the user selection changes. This should be listened by componens that want to do some work
     * when the user selection changes, regardles of the type of selected document.
     */
    public static final String USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED = "userAllDocumentTypesSelectionChanged";

    /**
     * Fired when the selected domain changes. Should be listened by components interested specifically in domain
     * selection change.
     */
    public static final String DOMAIN_SELECTION_CHANGED = "domainSelectionChanged";

    /**
     * Fired when content root selection is changed (like workspaces root, section root).
     */
    public static final String CONTENT_ROOT_SELECTION_CHANGED = "contentRootSelectionChanged";

    /**
     * Fired when a document selection changes ( file, folder etc not workspace or above ).
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
     * Fired after navigating to a document, the document is passed as argument.
     *
     * @since 5.4.2
     */
    public static final String NAVIGATE_TO_DOCUMENT = "navigateToDocument";

    /**
     * Should be raised before an edited document is saved.
     *
     * @since 5.4.2
     */
    public static final String BEFORE_DOCUMENT_CHANGED = "beforeDocumentChanged";

    /**
     * Fired after a document is locked, the document is passed as argument.
     *
     * @since 5.4.2
     */
    public static final String DOCUMENT_LOCKED = "documentLocked";

    /**
     * Fired after a document is unlocked, the document is passed as argument.
     *
     * @since 5.4.2
     */
    public static final String DOCUMENT_UNLOCKED = "documentUnlocked";

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
     * Should be raised when the user goes Home. This is useful to allow any components to revert to their uninitialized
     * state if needed.
     * <p>
     * Ex: The tree and breadcrumb should not display anything because home is view_servers and they display what's
     * inside a domain.
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

    /**
     * Event raised when the content of a directory has changed
     *
     * @since 5.5
     */
    public static final String DIRECTORY_CHANGED = "directoryChanged";

    public static final String USER_SESSION_STARTED = "org.nuxeo.ecm.web.userSessionStarted";

    /**
     * Flush event sent to the Seam layer, only when using the dev mode, and useful for components to reset their cache
     * smoothly: Seam components should not be destroyed completely.
     *
     * @since 5.6
     * @see Framework#isDevModeSet()
     */
    public static final String FLUSH_EVENT = "flush";

    // Constant utility class.
    private EventNames() {
    }

}
