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

package org.nuxeo.ecm.platform.cache.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.cache.CacheListener;

/**
 * A CacheListener implementation to be used in unit tests.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class ReportingCacheListener implements CacheListener {

    public static final String EVT_DOCUMENT_REMOVE = "documentRemove";

    public static final String EVT_DOCUMENT_REMOVED = "documentRemoved";

    public static final String EVT_DOCUMENT_UPDATE = "documentUpdate";

    private static final Log log = LogFactory.getLog(ReportingCacheListener.class);

    private final List<String> receivedEvents = new ArrayList<String>();

    private String lastEvent;


    public String getAndConsumeLastEvent() {
        String evt = lastEvent;
        lastEvent = null;
        return evt;
    }

    public List<String> getReceivedEvents() {
        return receivedEvents;
    }

    public void clearReceivedEvents() {
        receivedEvents.clear();
    }

    // CacheListener impl - start
    public void documentRemove(DocumentModel docModel) {
        setLastEvent(EVT_DOCUMENT_REMOVE, docModel.getPathAsString());
    }

    public void documentRemoved(String fqn) {
        setLastEvent(EVT_DOCUMENT_REMOVED, fqn);
    }

    public void documentUpdate(DocumentModel docModel, boolean pre) {
        setLastEvent(EVT_DOCUMENT_UPDATE, docModel.getPathAsString());
    }
    // CacheListener impl - end

    private void setLastEvent(String evt, String id) {
        if (lastEvent != null) {
            log.info("OVERRIDE EVT: " + lastEvent);
        }
        lastEvent = evt;
        receivedEvents.add(evt);
        log.info("EVT: " + lastEvent + "; id: " + id);
    }
}
