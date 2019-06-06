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
 *
 * $Id: CoreEventConstants.java 29901 2008-02-05 17:01:22Z ogrisel $
 */

package org.nuxeo.ecm.core.api.event;

/**
 * Core event constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class CoreEventConstants {

    public static final String DOC_LIFE_CYCLE = "documentLifeCycle";

    /**
     * BBB for NXP-666: change events to manage DocumentModel instances instead of Document instances.
     * <p>
     * Document is passed as an option in event in case old listeners need it and cannot handle the document model.
     */
    // public static final String DOCUMENT = "document";

    /**
     * Path the of the container of the empty document model that is being created.
     */
    public static final String PARENT_PATH = "parentPath";

    /**
     * @deprecated since 7.1, use {@link CoreEventConstants#DESTINATION_NAME} instead.
     */
    @Deprecated
    public static final String DOCUMENT_MODEL_ID = "documentModelId";

    public static final String REPOSITORY_NAME = "repositoryName";

    public static final String SESSION_ID = "sessionId";

    public static final String OLD_ACP = "oldACP";

    public static final String NEW_ACP = "newACP";

    /**
     * @since 7.4
     */
    public static final String OLD_ACE = "oldACE";

    /**
     * @since 7.4
     */
    public static final String NEW_ACE = "newACE";

    /**
     * @since 7.4
     */
    public static final String CHANGED_ACL_NAME = "changedACLName";

    public static final String REORDERED_CHILD = "reorderedChild";

    public static final String REPLACED_PROXY_IDS = "replacedProxyRefs";

    /**
     * @since 7.4
     */
    public static final String DOCUMENT_REFS = "documentRefs";

    /**
     * Passed with beforeDocumentModification and documentModified events to hold the state that is about to be / has
     * been overwritten by the saveDocument.
     */
    public static final String PREVIOUS_DOCUMENT_MODEL = "previousDocumentModel";

    /**
     * Passed with aboutToCopy, aboutToMove, documentCreatedbyCopy and documentMoved events to be able to change the
     * destination name
     *
     * @since 5.7
     */
    public static final String DESTINATION_NAME = "destinationName";

    public static final String DESTINATION_REF = "destinationRef";

    public static final String DESTINATION_PATH = "destinationPath";

    public static final String DESTINATION_EXISTS = "destinationExists";

    public static final String DOCUMENT_DIRTY = "documentIsDirty";

    public static final String SOURCE_REF = "sourceRef";

    /**
     * Passed with documentMoved event, if the name has changed, to know the original name of the document.
     *
     * @since 7.3
     */
    public static final String ORIGINAL_NAME = "originalName";

    /**
     * Passed with documentCreatedbyCopy event to be able to reset the life cycle or not
     *
     * @since 5.7
     */
    public static final String RESET_LIFECYCLE = "resetLifeCycle";

    /**
     * Passed with documentCreatedByCopy event to be able to reset creator, creation date and last modification date or
     * not
     *
     * @since 8.2
     */
    public static final String RESET_CREATOR = "resetCreator";

    /**
     * Passed with {@value DocumentEventTypes#BEFORE_SET_RETENTION} and {@value DocumentEventTypes#AFTER_SET_RETENTION}
     * events, the retention datetime (a {@link Calendar} object).
     *
     * @since 11.1
     */
    public static final String RETAIN_UNTIL = "retainUntil";

    /**
     * Passed with {@value DocumentEventTypes#BEFORE_SET_LEGAL_HOLD} and
     * {@value DocumentEventTypes#AFTER_SET_LEGAL_HOLD} events, the legal hold status (a {@link Boolean} object).
     *
     * @since 11.1
     */
    public static final String LEGAL_HOLD = "legalHold";

    /**
     * Passed with retentionActiveChanged event, status of the retention (active or not, a Boolean).
     *
     * @since 9.3
     * @deprecated since 11.1
     */
    @Deprecated
    public static final String RETENTION_ACTIVE = "retentionActive";

    // Constant utility class
    private CoreEventConstants() {
    }

}
