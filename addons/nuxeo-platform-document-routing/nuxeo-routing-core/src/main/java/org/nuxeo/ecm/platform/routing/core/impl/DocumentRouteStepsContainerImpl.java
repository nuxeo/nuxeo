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
