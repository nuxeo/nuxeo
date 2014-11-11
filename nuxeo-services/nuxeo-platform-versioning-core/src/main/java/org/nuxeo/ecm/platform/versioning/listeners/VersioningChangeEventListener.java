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
 */

package org.nuxeo.ecm.platform.versioning.listeners;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningChangeNotifier;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.versioning.service.VersioningModifierService;
import org.nuxeo.runtime.api.Framework;

/**
 * This listener is interested in events fired from Versioning component when
 * the version of a document was changed (i.e. a new doc version was created
 * through checkin-checkout process).
 *
 * @author DM
 * @deprecated remove in 5.2
 */
// XXX AT: this is useless now, event is caught but not processed
@Deprecated
public class VersioningChangeEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(VersioningChangeEventListener.class);

    public void handleEvent(Event event) throws ClientException {

         if (VersioningChangeNotifier.CORE_EVENT_ID_VERSIONING_CHANGE.equals(event.getName())) {


             // get event info containing document references
             final Map<String, Serializable> info = event.getContext().getProperties();
             final DocumentModel newDoc = (DocumentModel) info.get(VersioningChangeNotifier.EVT_INFO_NEW_DOC_KEY);
             final DocumentModel oldDoc = (DocumentModel) info.get(VersioningChangeNotifier.EVT_INFO_OLD_DOC_KEY);

             /*
             if (newDoc != null) {
                 log.debug(logPrefix + "new doc ref=" + newDoc.getRef());
             } else {
                 log.debug(logPrefix + "new doc is null");
             }
             if (oldDoc != null) {
                 log.debug(logPrefix + "old doc ref=" + oldDoc.getRef());
             } else {
                 log.debug(logPrefix + "old doc is null");
             }*/

             try {
                 versionsChangeNotify(newDoc, oldDoc);
             } catch (Exception e) {
                 log.error("Error processing versions change notification", e);
             }
         }
    }

    /**
     * Could be overridden to perform custom processing. By default it calls
     * VersioningModifierService.doModifications(newDoc)
     *
     * @param newDoc
     * @param oldDoc
     */
    protected void versionsChangeNotify(final DocumentModel newDoc,
            final DocumentModel oldDoc) {
        // Call the EP here
        VersioningModifierService service = (VersioningModifierService) Framework
                .getRuntime().getComponent(VersioningModifierService.NAME);

        service.doModifications(newDoc);
    }
}
