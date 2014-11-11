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

package org.nuxeo.ecm.core.api.event;

/**
 * TODO: write description.
 *
 * @author DM
 */
// TODO a better id-zation is to define concrete instances of a "EventType"
// class, so the id-s won't be checked against ordinar strings. This has to be
// redefined in CoreEvent API
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

    @Deprecated
    public static final String ABOUT_TO_INITIALIZE = "aboutToInitialize";

    @Deprecated
    public static final String DOCUMENT_INITIALIZED = "documentInitialized";

    public static final String ABOUT_TO_REMOVE = "aboutToRemove";

    public static final String DOCUMENT_REMOVED = "documentRemoved";

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

    public static final String SECTION_CONTENT_PUBLISHED = "sectionContentPublished";

    public static final String BEFORE_DOC_RESTORE = "beforeRestoringDocument";

    public static final String DOCUMENT_RESTORED = "documentRestored";

    public static final String SESSION_SAVED = "sessionSaved";

    public static final String DOCUMENT_CHILDREN_ORDER_CHANGED = "childrenOrderChanged";

    /** This event is too general and should be used with care. */
    public static final String ABOUT_TO_CHECKOUT = "aboutToCheckout";

    /** This event is too general and should be used with care. */
    public static final String DOCUMENT_CHECKEDOUT = "aboutToCheckedOut";

    /** This event is too general and should be used with care. */
    public static final String ABOUT_TO_CHECKIN = "aboutToCheckIn";

    /** This event is too general and should be used with care. */
    public static final String DOCUMENT_CHECKEDIN = "documentCheckedIn";

    public static final String SUBSCRIPTION_ASSIGNED = "subscriptionAssigned";

    public static final String EMAIL_DOCUMENT_SEND = "emailDocumentSend";

    // Constant utility class
    private DocumentEventTypes() {
    }

}
