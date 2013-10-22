/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.event;

/**
 * TODO: write description.
 *
 * @author DM
 */
// TODO a better id-zation is to define concrete instances of a "EventType"
// class, so the id-s won't be checked against ordinar strings. This has to be
// redefined in CoreEvent API
// TODO ...or use enums...
public final class DocumentEventTypes {

    public static final String ABOUT_TO_CREATE = "aboutToCreate";

    /**
     * Empty document mode created by the DocumentModelFactory.
     * <p>
     * Useful for initialization of the fields with computed contextual data.
     */
    public static final String EMPTY_DOCUMENTMODEL_CREATED = "emptyDocumentModelCreated";

    /**
     * At this point the document is filled with data from DocumentModel.
     */
    public static final String DOCUMENT_CREATED = "documentCreated";

    /** @since 5.8 **/
    public static final String ABOUT_TO_IMPORT = "aboutToImport";

    public static final String DOCUMENT_IMPORTED = "documentImported";

    public static final String ABOUT_TO_REMOVE = "aboutToRemove";

    public static final String DOCUMENT_REMOVED = "documentRemoved";

    public static final String ABOUT_TO_REMOVE_VERSION = "aboutToRemoveVersion";

    public static final String VERSION_REMOVED = "versionRemoved";

    public static final String BEFORE_DOC_UPDATE = "beforeDocumentModification";

    public static final String BEFORE_DOC_SECU_UPDATE = "beforeDocumentSecurityModification";

    public static final String DOCUMENT_UPDATED = "documentModified";

    public static final String DOCUMENT_SECURITY_UPDATED = "documentSecurityUpdated";

    public static final String DOCUMENT_LOCKED = "documentLocked";

    public static final String DOCUMENT_UNLOCKED = "documentUnlocked";

    public static final String ABOUT_TO_COPY = "aboutToCopy";

    public static final String DOCUMENT_CREATED_BY_COPY = "documentCreatedByCopy";

    public static final String DOCUMENT_DUPLICATED = "documentDuplicated";

    public static final String ABOUT_TO_MOVE = "aboutToMove";

    public static final String DOCUMENT_MOVED = "documentMoved";

    public static final String DOCUMENT_PUBLISHED = "documentPublished";

    public static final String DOCUMENT_PROXY_PUBLISHED = "documentProxyPublished";

    public static final String DOCUMENT_PROXY_UPDATED = "documentProxyUpdated";

    public static final String SECTION_CONTENT_PUBLISHED = "sectionContentPublished";

    public static final String BEFORE_DOC_RESTORE = "beforeRestoringDocument";

    public static final String DOCUMENT_RESTORED = "documentRestored";

    public static final String SESSION_SAVED = "sessionSaved";

    public static final String DOCUMENT_CHILDREN_ORDER_CHANGED = "childrenOrderChanged";

    /** This event is too general and should be used with care. */
    public static final String ABOUT_TO_CHECKOUT = "aboutToCheckout";

    /**
     * Document checked out. Listeners can increment version numbers. Listeners
     * will be passed a pristine DocumentModel where changes will not be seen by
     * the main DocumentModel being saved.
     */
    public static final String DOCUMENT_CHECKEDOUT = "documentCheckedOut";

    /**
     * Listeners can increment version numbers. Listeners will be passed a
     * pristine DocumentModel where changes will not be seen by the main
     * DocumentModel being saved.
     */
    public static final String INCREMENT_BEFORE_UPDATE = "incrementBeforeUpdate";

    /** This event is too general and should be used with care. */
    public static final String ABOUT_TO_CHECKIN = "aboutToCheckIn";

    /** This event is too general and should be used with care. */
    public static final String DOCUMENT_CHECKEDIN = "documentCheckedIn";

    public static final String SUBSCRIPTION_ASSIGNED = "subscriptionAssigned";

    public static final String EMAIL_DOCUMENT_SEND = "emailDocumentSend";

    /**
     * Event triggered when a personal user workspace is created
     *
     * @since 5.7
     */
    public static final String USER_WORKSPACE_CREATED = "userWorkspaceCreated";

    // Constant utility class
    private DocumentEventTypes() {
    }

}
