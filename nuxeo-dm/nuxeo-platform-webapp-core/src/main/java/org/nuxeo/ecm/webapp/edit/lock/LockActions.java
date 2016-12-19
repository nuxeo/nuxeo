/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.edit.lock;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;

/**
 * Interface for an action listener that will provide methods to lock/unlock a document, to lock/unlock the current
 * document and to lock/unlock a list of documents (based on DocumentsListManager).
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public interface LockActions extends Serializable {

    String LOCKER = "document.locker";

    /** @since 5.4.2 */
    String LOCK_CREATED = "document.lock.created";

    /**
     * Gets the lock of the current document.
     *
     */
    String lockCurrentDocument();

    /**
     * Releases the lock of the current document.
     *
     */
    String unlockCurrentDocument();

    /**
     * Gets the lock of the document.
     *
     * @param document the document of which lock is to be taken
     */
    String lockDocument(DocumentModel document);

    /**
     * Releases the lock of the document.
     *
     * @param document the document of which lock is to be released
     */
    String unlockDocument(DocumentModel document);

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
     * Gets the details about the lock of a document,who did the lock and when the lock took place.
     *
     * @param document the document for which this information is needed
     * @return the user who took the look and the time when he/she did this in a map
     */
    Map<String, Serializable> getLockDetails(DocumentModel document);

    /**
     * Gets the details about the lock of the current document, who did the lock and when the lock took place.
     *
     * @return the user who took the look and the time when he/she did this in a map
     */
    Map<String, Serializable> getCurrentDocLockDetails();

    void resetLockState();

}
