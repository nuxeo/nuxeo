/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author arussel
 *
 */
public class DocumentRouteImpl extends DocumentRouteStepsContainerImpl
        implements DocumentRoute {
    private static final long serialVersionUID = 1L;

    @Override
    public void setDone(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toDone, session, false);
        EventFirer.fireEvent(session, this, null,
                DocumentRoutingConstants.Events.afterRouteFinish.name());
        // Fire events for route audit log
        for (String attachDocumentID : this.getAttachedDocuments()) {
            AuditEventFirer.fireEvent(session, this, null, "auditLogRoute",
                    attachDocumentID);
        }
    }

    public DocumentRouteImpl(DocumentModel doc, ElementRunner runner) {
        super(doc, runner);
    }

    @Override
    public boolean canUndoStep(CoreSession session) {
        return false;
    }

}
