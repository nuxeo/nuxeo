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
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStepsContainer;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author arussel
 *
 */
public class DocumentRouteStepsContainerImpl extends DocumentRouteElementImpl
        implements DocumentRouteStepsContainer {

    public DocumentRouteStepsContainerImpl(DocumentModel doc) {
        super(doc);
    }

    public void setAttachedDocuments(List<String> documentIds) {
        try {
            document.setPropertyValue(
                    DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                    (Serializable) documentIds);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getAttachedDocuments() {
        try {
            return (List<String>) document.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected List<DocumentRouteElement> getChildrenElement(CoreSession session) {
        try {
            DocumentModelList children = session.getChildren(document.getRef());
            List<DocumentRouteElement> elements = new ArrayList<DocumentRouteElement>();
            for (DocumentModel model : children) {
                elements.add(model.getAdapter(DocumentRouteElement.class));
            }
            return elements;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }
}
