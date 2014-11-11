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

package org.nuxeo.ecm.core.api;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * When a conflict is detected (transaction in progress on the given docRef) we
 * wait the end of the first document incremented and we checkin / checkout it
 * in another transaction and we finish the second transaction
 * 
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
public class ConflictVersioningDetector {

    private static final Log log = LogFactory.getLog(ConflictVersioningDetector.class);

    private enum ConflictStateEnum {
        NO_CONFLICT_DETECTED, CONFLICT_DETECTED_AND_PREVIOUS_TRANSACTION_FINISHED, CONFLICT_DETECTED_AND_PREVIOUS_TRANSACTION_NOT_FINISHED
    }

    /*
     * These two maps store last documents modified with minor and respectively
     * major version number and last modification date. This is used for detect
     * more than one automatic update version in the same time. (see in
     * incrementMajor and incrementMinor method)
     */
    private static final Map<DocumentRef, Item> lastDocumentsIncremented = new ConcurrentHashMap<DocumentRef, Item>();

    private boolean conflictDetected = false;

    private DocumentModel doc;

    private DocumentRef docRef;

    private long lastModification;

    public ConflictVersioningDetector(DocumentModel doc)
            throws PropertyException {
        this.doc = doc;
        docRef = doc.getRef();
        lastModification = ((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis();
    }

    public DocumentModel deconflictIncrementVersion() throws Exception {

        // No two deconflictions in the same time on a same document
        // So wait the "end" of the transaction
        log.debug("Start conflict Detection of docRef : " + docRef);
        ConflictStateEnum conflictState = ConflictVersioningDetector.isOtherConflictTransactionInProgress(
                docRef, lastModification);
        while (conflictState.equals(ConflictStateEnum.CONFLICT_DETECTED_AND_PREVIOUS_TRANSACTION_NOT_FINISHED)) {
            log.debug("Waiting for unlock docRef : " + docRef);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("InterruptedException sent : ignored", e);
            }
            conflictState = ConflictVersioningDetector.isOtherConflictTransactionInProgress(
                    docRef, lastModification);
        }
        ;

        if (conflictState.equals(ConflictStateEnum.NO_CONFLICT_DETECTED)) {
            log.debug("No conflict decteted");
            conflictDetected = false;
            return doc;
        }

        log.warn("Conflict detected for doc " + docRef
                + ", deconfliction started");
        conflictDetected = true;

        // unlock is done on the commit of the ConflictResolver, but we need to
        // be sure that the document
        // checkined is in database
        Thread.sleep(7000);

        ConflictResolver cr = new ConflictResolver(docRef,
                doc.getRepositoryName());
        // Checkin / Checkout in other transaction the previous document
        // checkouted
        cr.start();
        cr.join();

        // Set the correct last version of the document in this context
        log.debug("Set version " + cr.getMajor() + "." + cr.getMinor()
                + " to doc " + docRef);

        Long minor = cr.getMinor();
        Long major = cr.getMajor();

        if (minor == null && major == null) {
            minor = new Long(1);
            major = new Long(0);
        }

        doc.setPropertyValue("uid:major_version", major);
        doc.setPropertyValue("uid:minor_version", minor);

        log.debug("End conflict Detection of docRef : " + docRef);

        return doc;
    }

    public void setConflictTransactionFinished() {
        log.debug("unlock docRef : " + docRef);
        Item item = lastDocumentsIncremented.get(docRef);
        item.unlockConflictTransactionInProgress();
    }

    private static synchronized ConflictStateEnum isOtherConflictTransactionInProgress(
            DocumentRef docRef, long lastModification) throws Exception {
        Item item = lastDocumentsIncremented.get(docRef);

        if (item != null) {
            if (!item.lockConflictTransactionInProgress()) {
                return ConflictStateEnum.CONFLICT_DETECTED_AND_PREVIOUS_TRANSACTION_NOT_FINISHED;
            } else {
                item.lockConflictTransactionInProgress();
                // Be sure that the previous document is not the same
                if (item.getLastModificationDate() == lastModification) {
                    return ConflictStateEnum.CONFLICT_DETECTED_AND_PREVIOUS_TRANSACTION_FINISHED;
                }
                item.setLastModificationDate(lastModification);
                return ConflictStateEnum.NO_CONFLICT_DETECTED;
            }
        }

        item = new Item();
        item.lockConflictTransactionInProgress();
        item.setLastModificationDate(lastModification);

        lastDocumentsIncremented.put(docRef, item);
        return ConflictStateEnum.NO_CONFLICT_DETECTED;
    }

    public boolean isConflictDetected() {
        return conflictDetected;
    }

}

class Item {
    public long deconflictionInProgressLock = 0;

    public long conflictTransactionInProgressLock = 0;

    public long lastModificationDate = 0;

    public synchronized boolean lockDeconflictionInProgress() {
        if (deconflictionInProgressLock == 0) {
            deconflictionInProgressLock++;
            return true;
        }
        return false;
    }

    public void unlockDeconflictionInProgress() {
        deconflictionInProgressLock--;
    }

    public synchronized boolean lockConflictTransactionInProgress() {
        if (conflictTransactionInProgressLock == 0) {
            conflictTransactionInProgressLock++;
            return true;
        }
        return false;
    }

    public void unlockConflictTransactionInProgress() {
        conflictTransactionInProgressLock--;
    }

    public void setLastModificationDate(long lastModification) {
        this.lastModificationDate = lastModification;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }
}
