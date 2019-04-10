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

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStepsContainer;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author arussel
 */
public class DocumentRouteStepsContainerImpl extends DocumentRouteElementImpl implements DocumentRouteStepsContainer {

    private static final long serialVersionUID = 1L;

    public DocumentRouteStepsContainerImpl(DocumentModel doc, ElementRunner runner) {
        super(doc, runner);
    }

    public void setAttachedDocuments(List<String> documentIds) {
        document.setPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                (Serializable) documentIds);
    }

    @SuppressWarnings("unchecked")
    public List<String> getAttachedDocuments() {
        return (List<String>) document.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME);
    }

    @Override
    public void setDone(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toDone, session, false);
    }

    @Override
    public void validate(CoreSession session) {
        // validate this routeModel
        EventFirer.fireEvent(session, this, null, DocumentRoutingConstants.Events.beforeRouteValidated.name());
        setValidated(session);
        EventFirer.fireEvent(session, this, null, DocumentRoutingConstants.Events.afterRouteValidated.name());
        setReadOnly(session);
    }
}
