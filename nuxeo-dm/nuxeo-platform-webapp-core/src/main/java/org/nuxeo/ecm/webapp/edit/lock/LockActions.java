/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.edit.lock;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;

/**
 * Interface for an action listener that will provide methods to lock/unlock a
 * document, to lock/unlock the current document and to lock/unlock a list of
 * documents (based on DocumentsListManager).
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public interface LockActions extends Serializable {

     String LOCKER = "document.locker";

     /** @deprecated since 5.4.2, use {@link #LOCK_CREATED} instead */
     @Deprecated
     String LOCK_TIME = "document.lock.time";

     /** @since 5.4.2 */
     String LOCK_CREATED = "document.lock.created";

    /**
     * Gets the lock of the current document.
     *
     * @throws ClientException
     */
    String lockCurrentDocument() throws ClientException;

    /**
     * Releases the lock of the current document.
     *
     * @throws ClientException
     */
    String unlockCurrentDocument() throws ClientException;

    /**
     * Gets the lock of the document.
     *
     * @param document the document of which lock is to be taken
     *
     * @throws ClientException
     */
    String lockDocument(DocumentModel document) throws ClientException;

    /**
     * Releases the lock of the document.
     *
     * @param document the document of which lock is to be released
     *
     * @throws ClientException
     */
    String unlockDocument(DocumentModel document) throws ClientException;

    /**
     * Gets the locks of the documents from the list.
     *
     * @param documents the list with the documents of which locks are to be
     *            taken
     *
     * @throws ClientException
     */
    void lockDocuments(List<DocumentModel> documents) throws ClientException;

    /**
     * Releases the locks of the documents from the list.
     *
     * @param documents the list with the documents of which locks are to be
     *            released
     *
     * @throws ClientException
     */
    void unlockDocuments(List<DocumentModel> documents) throws ClientException;

    /**
     * Tests if the user can get the lock of a document.
     *
     * @return true if the user has this right, false otherwise
     */
    Boolean getCanLockDoc(DocumentModel document);

    /**
     * Tests if the user can get the lock of the current document.
     *
     * @return true if the user has this right, false otherwise
     */
    Boolean getCanLockCurrentDoc();

    /**
     * Tests if the user can unlock a document.
     *
     * @return true if the user has this right, false otherwise
     */
    Boolean getCanUnlockDoc(DocumentModel document);

    /**
     * Tests if the user can unlock the current document.
     *
     * @return true if the user has this right, false otherwise
     */
    Boolean getCanUnlockCurrentDoc();

    /**
     * Returns the action of lock or unlock for a document.
     *
     * @return the action of lock or unlock for a document
     */
    Action getLockOrUnlockAction();

    /**
     * Gets the details about the lock of a document,who did the lock and when
     * the lock took place.
     *
     * @param document the document for which this information is needed
     * @return the user who took the look and the time when he/she did this in a
     *         map
     * @throws ClientException
     */
    Map<String, Serializable> getLockDetails(DocumentModel document)
            throws ClientException;

    /**
     * Gets the details about the lock of the current document, who did the lock
     * and when the lock took place.
     *
     * @return the user who took the look and the time when he/she did this in a
     *         map
     * @throws ClientException
     */
    Map<String, Serializable> getCurrentDocLockDetails() throws ClientException;

    /**
     * This method is used to test whether the live-edit link should appear on a
     * document.
     *
     * @return true if the link can appear,
     *         false otherwise
     */
    Boolean isCurrentDocumentLiveEditable();

    void resetLockState();

    void destroy();

}
