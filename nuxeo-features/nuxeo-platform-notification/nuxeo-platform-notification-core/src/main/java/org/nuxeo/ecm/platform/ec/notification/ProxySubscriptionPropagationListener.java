/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ec.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;

/**
 * Propagate previously set notifications when a proxy is replaced by a new
 * version.
 *
 * @author ogrisel
 */
public class ProxySubscriptionPropagationListener implements EventListener {

    private static final Log log = LogFactory.getLog(ProxySubscriptionPropagationListener.class);

    @SuppressWarnings("unchecked")
    public void handleEvent(Event event) throws ClientException {

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

        List<String> replacedProxyIds = (List<String>) ctx.getProperties().get(
                CoreEventConstants.REPLACED_PROXY_IDS);
        if (replacedProxyIds == null) {
            return;
        }

        for (String replacedProxyId : replacedProxyIds) {
            // there should be only one replaced proxy, but just in case,
            // iterate over them
            try {
                propagateSubscription(replacedProxyId,
                        publishedDoc.getRef().toString());
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
    }

    protected void propagateSubscription(String fromDocId, String toDocId)
            throws Exception {
        PlacefulService service = NotificationServiceHelper.getPlacefulService();
        String className = service.getAnnotationRegistry().get(
                NotificationService.SUBSCRIPTION_NAME);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        if (fromDocId != null) {
            paramMap.put("docId", fromDocId);
        }
        PlacefulService placefulService = NotificationServiceHelper.getPlacefulServiceBean();

        List<Annotation> subscriptions = placefulService.getAnnotationListByParamMap(
                paramMap, shortClassName);
        for (Object obj : subscriptions) {
            UserSubscription subscription = (UserSubscription) obj;
            subscription.setDocId(toDocId);
            placefulService.setAnnotation(subscription);
        }
    }
}
