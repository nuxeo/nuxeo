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

package org.nuxeo.ecm.multi.tenant;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        String tenantDocumentType = multiTenantService.getTenantDocumentType();
        if (tenantDocumentType == null) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        if (DOCUMENT_CREATED.equals(event.getName())) {
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession session = docCtx.getCoreSession();
            if (tenantDocumentType.equals(doc.getType())) {

                if (multiTenantService.isTenantIsolationEnabled(session)) {
                    multiTenantService.enableTenantIsolationFor(session, doc);
                    session.save();
                }
            }
        } else if (DOCUMENT_REMOVED.equals(event.getName())) {
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession session = docCtx.getCoreSession();
            if (tenantDocumentType.equals(doc.getType())) {
                if (multiTenantService.isTenantIsolationEnabled(session)) {
                    multiTenantService.disableTenantIsolationFor(session, doc);
                    session.save();
                }
            }
        }
    }

}
