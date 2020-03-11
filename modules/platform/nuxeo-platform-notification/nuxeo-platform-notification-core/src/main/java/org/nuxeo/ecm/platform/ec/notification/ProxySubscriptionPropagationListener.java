/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ec.notification;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;

/**
 * Propagate previously set notifications when a proxy is replaced by a new version.
 *
 * @author ogrisel
 */
public class ProxySubscriptionPropagationListener implements EventListener {

    private static final Log log = LogFactory.getLog(ProxySubscriptionPropagationListener.class);

    @Override
    @SuppressWarnings("unchecked")
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();

        if (!(ctx instanceof DocumentEventContext)) {
            // we are only interested in propagating notification for document
            // proxies
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel publishedDoc = docCtx.getSourceDocument();
        if (!publishedDoc.isProxy()) {
            // we are only interested in the publication of proxy documents
            return;
        }

        NotificationService service = NotificationServiceHelper.getNotificationService();
        if (service == null) {
            log.error("Unable to get NotificationService, exiting");
            return;
        }

        List<String> replacedProxyIds = (List<String>) ctx.getProperties().get(CoreEventConstants.REPLACED_PROXY_IDS);
        if (replacedProxyIds == null) {
            return;
        }

        for (String replacedProxyId : replacedProxyIds) {
            // there should be only one replaced proxy, but just in case,
            // iterate over them
            DocumentModel fromDoc = ctx.getCoreSession().getDocument(new IdRef(replacedProxyId));
            fromDoc.getAdapter(SubscriptionAdapter.class).copySubscriptionsTo(publishedDoc);
        }
        ctx.getCoreSession().saveDocument(publishedDoc);
    }


}
