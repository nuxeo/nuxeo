/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    public static final String DOCUMENT_REMOVAL_CANCELED = "documentRemovalCanceled";

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
     * Document checked out. Listeners can increment version numbers. Listeners will be passed a pristine DocumentModel
     * where changes will not be seen by the main DocumentModel being saved.
     */
    public static final String DOCUMENT_CHECKEDOUT = "documentCheckedOut";

    /**
     * Listeners can increment version numbers. Listeners will be passed a pristine DocumentModel where changes will not
     * be seen by the main DocumentModel being saved.
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

    /**
     * A binary fulltext field has been updated.
     *
     * @since 5.9.3
     */
    public static final String BINARYTEXT_UPDATED = "binaryTextUpdated";

    /**
     * The event property containing the system property updated.
     * <p>
     * Available for the event {@link #BINARYTEXT_UPDATED}.
     *
     * @since 10.3
     */
    public static final String SYSTEM_PROPERTY = "systemProperty";

    /**
     * The event property containing the value of the system property updated.
     * <p>
     * Available for the event {@link #BINARYTEXT_UPDATED}.
     *
     * @since 10.3
     */
    public static final String SYSTEM_PROPERTY_VALUE = "systemPropertyValue";

    /**
     * @since 6.0
     */
    public static final String DOCUMENT_TAG_UPDATED = "documentTagUpdated";

    /**
     * Event triggered when one or more ACE status have been updated.
     *
     * @since 7.4
     */
    public static final String ACE_STATUS_UPDATED = "ACEStatusUpdated";

    /**
     * Event triggered when the active state of the retention changes.
     *
     * @since 9.3
     */
    public static final String RETENTION_ACTIVE_CHANGED = "retentionActiveChanged";

    // Constant utility class
    private DocumentEventTypes() {
    }

}
