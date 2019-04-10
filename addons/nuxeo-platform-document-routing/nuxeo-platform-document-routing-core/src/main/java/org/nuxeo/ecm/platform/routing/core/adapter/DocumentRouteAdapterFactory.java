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
package org.nuxeo.ecm.platform.routing.core.adapter;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues;
import org.nuxeo.ecm.platform.routing.core.impl.ConditionalRunner;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRouteElementImpl;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRouteImpl;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRouteStepsContainerImpl;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNodeImpl;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRouteImpl;
import org.nuxeo.ecm.platform.routing.core.impl.ParallelRunner;
import org.nuxeo.ecm.platform.routing.core.impl.SerialRunner;
import org.nuxeo.ecm.platform.routing.core.impl.StepElementRunner;

/**
 * Provides {@link DocumentRoute} for a {@link DocumentModel}.
 *
 * @author arussel
 *
 */
public class DocumentRouteAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc,
            @SuppressWarnings("rawtypes") Class itf) {
        String type = doc.getType();
        if (doc.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
            ExecutionTypeValues executionType = getExecutionType(doc, type);
            switch (executionType) {
            case serial:
                return new DocumentRouteImpl(doc, new SerialRunner());
            case parallel:
                return new DocumentRouteImpl(doc, new ParallelRunner());
            case graph:
                return new GraphRouteImpl(doc);
            }
        } else if (type.equals("RouteNode")) {
            return new GraphNodeImpl(doc, null);
        } else if (doc.hasFacet(DocumentRoutingConstants.ROUTE_STEP_FACET)) {
            return new DocumentRouteElementImpl(doc, new StepElementRunner());
        } else if (doc.hasFacet(DocumentRoutingConstants.CONDITIONAL_STEP_FACET)) {
            return new DocumentRouteStepsContainerImpl(doc,
                    new ConditionalRunner());
        } else if (doc.hasFacet(DocumentRoutingConstants.STEP_FOLDER_FACET)) {
            ExecutionTypeValues executionType = getExecutionType(doc, type);
            switch (executionType) {
            case serial:
                return new DocumentRouteStepsContainerImpl(doc,
                        new SerialRunner());
            case parallel:
                return new DocumentRouteStepsContainerImpl(doc,
                        new ParallelRunner());
            case graph:
                throw new UnsupportedOperationException();
            }
        }
        return null;
    }

    protected ExecutionTypeValues getExecutionType(DocumentModel doc,
            String type) {
        ExecutionTypeValues executionType = ExecutionTypeValues.valueOf((String) getProperty(
                doc, DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME));
        return executionType;
    }

    protected Object getProperty(DocumentModel doc, String xpath) {
        try {
            return doc.getPropertyValue(xpath);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}
