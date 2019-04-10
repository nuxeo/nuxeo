/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener handling default events to update statistics through the
 * {@link org.nuxeo.ecm.quota.QuotaStatsService}.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsListener implements EventListener {

    public static final Set<String> EVENTS_TO_HANDLE = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(DOCUMENT_CREATED, DOCUMENT_CREATED_BY_COPY,
                    DOCUMENT_UPDATED, DOCUMENT_MOVED, ABOUT_TO_REMOVE,
                    BEFORE_DOC_UPDATE, ABOUT_TO_REMOVE_VERSION)));

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (EVENTS_TO_HANDLE.contains(event.getName())) {
            EventContext ctx = event.getContext();
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
                quotaStatsService.updateStatistics(docCtx, event);
            }
        }
    }

}
