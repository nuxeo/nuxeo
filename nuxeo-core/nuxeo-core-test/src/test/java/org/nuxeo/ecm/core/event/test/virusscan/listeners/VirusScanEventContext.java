/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.event.test.virusscan.listeners;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.virusscan.VirusScanConsts;

/**
 * Custom EventContext used to propagate info between the synchronous listener
 * and the Asynchrous listener
 * <p/>
 * The Blob xpaths are propagated using a List of String inside the
 * {@link EventContext}.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class VirusScanEventContext extends DocumentEventContext {

    private static final long serialVersionUID = 1L;

    public static final String VIRUS_SCAN_BLOB_XPATHS = "blobXPaths";

    public static final String MARKER_KEY = "contextType";

    public static final String MARKER_VALUE = "SizeUpdateEventContext";

    protected VirusScanEventContext(DocumentEventContext evtCtx) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(),
                evtCtx.getSourceDocument(), evtCtx.getDestination());
        setProperty(MARKER_KEY, MARKER_VALUE);
    }

    public VirusScanEventContext(DocumentEventContext evtCtx,
            List<String> blobXPaths) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(),
                evtCtx.getSourceDocument(), evtCtx.getDestination());
        setProperty(MARKER_KEY, MARKER_VALUE);
        setProperty(VIRUS_SCAN_BLOB_XPATHS, (Serializable) blobXPaths);
    }

    public static VirusScanEventContext unwrap(DocumentEventContext docCtx) {
        if (MARKER_VALUE.equals(docCtx.getProperty(MARKER_KEY))) {
            VirusScanEventContext ctx = new VirusScanEventContext(docCtx);
            ctx.setProperties(docCtx.getProperties());
            return ctx;
        }
        return null;
    }

    public List<String> getBlobPaths() {
        return (List<String>) getProperty(VIRUS_SCAN_BLOB_XPATHS);
    }

    public Event newVirusScanEvent() {
        return newEvent(VirusScanConsts.VIRUS_SCAN_NEEDED_EVENT);
    }

}