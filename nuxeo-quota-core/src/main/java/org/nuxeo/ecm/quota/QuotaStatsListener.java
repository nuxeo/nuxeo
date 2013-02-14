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
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_RESTORED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_RESTORE;

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
                    BEFORE_DOC_UPDATE, ABOUT_TO_REMOVE_VERSION,
                    DOCUMENT_CHECKEDIN, DOCUMENT_CHECKEDOUT, TRANSITION_EVENT,
                    BEFORE_DOC_RESTORE, DOCUMENT_RESTORED)));

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        if (!EVENTS_TO_HANDLE.contains(event.getName())) {
            return;
        }
        if (TRANSITION_EVENT.equals(event.getName())
                && !isTrashOpEvent((DocumentEventContext) ctx)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        quotaStatsService.updateStatistics(docCtx, event);
    }

    protected boolean isTrashOpEvent(DocumentEventContext eventContext) {
        String transition = (String) eventContext.getProperties().get(
                TRANSTION_EVENT_OPTION_TRANSITION);
        if (transition != null
                && (DELETE_TRANSITION.equals(transition) || UNDELETE_TRANSITION.equals(transition))) {
            return true;
        }
        return false;
    }
}
