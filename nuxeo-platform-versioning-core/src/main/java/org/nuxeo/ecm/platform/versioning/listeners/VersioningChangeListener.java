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

package org.nuxeo.ecm.platform.versioning.listeners;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningChangeNotifier;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.DocumentModelEventListener;
import org.nuxeo.ecm.platform.versioning.service.VersioningModifierService;
import org.nuxeo.runtime.api.Framework;

/**
 * This listener is interested in events fired from Versioning component when
 * the version of a document was changed (i.e. a new doc version was created
 * through checkin-checkout process).
 *
 * @author DM
 */
// XXX AT: this is useless now, event is caught but not processed
public class VersioningChangeListener extends AbstractEventListener implements
        AsynchronousEventListener, DocumentModelEventListener {

    private static final Log log = LogFactory.getLog(VersioningChangeListener.class);

    /**
     * @param coreEvent instance thrown at core layer
     */
    @Override
    public void notifyEvent(CoreEvent coreEvent) {

        final String logPrefix = "<notifyEvent> ";

        if (VersioningChangeNotifier.CORE_EVENT_ID_VERSIONING_CHANGE.equals(coreEvent.getEventId())) {
            log.debug(logPrefix + "handling event: " + coreEvent);

            // get event info containing document references
            final Map<String, ?> info = coreEvent.getInfo();
            final DocumentModel newDoc = (DocumentModel) info.get(VersioningChangeNotifier.EVT_INFO_NEW_DOC_KEY);
            final DocumentModel oldDoc = (DocumentModel) info.get(VersioningChangeNotifier.EVT_INFO_OLD_DOC_KEY);

            if (newDoc != null) {
                log.debug(logPrefix + "new doc ref=" + newDoc.getRef());
            } else {
                log.debug(logPrefix + "new doc is null");
            }
            if (oldDoc != null) {
                log.debug(logPrefix + "old doc ref=" + oldDoc.getRef());
            } else {
                log.debug(logPrefix + "old doc is null");
            }

            try {
                versionsChangeNotify(newDoc, oldDoc);
            } catch (Exception e) {
                log.error("Error processing versions change notification", e);
            }
        }
    }

    /**
     * Could be overriden to perform custom processing. By default it
     * calls VersioningModifierService.doModifications(newDoc)
     *
     * @param newDoc
     * @param oldDoc
     */
    protected void versionsChangeNotify(final DocumentModel newDoc, final DocumentModel oldDoc) {
        //Call the EP here
        VersioningModifierService service = (VersioningModifierService) Framework
                .getRuntime().getComponent(VersioningModifierService.NAME);

        service.doModifications(newDoc);

    }
}
