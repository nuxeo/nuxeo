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
package org.nuxeo.ecm.platform.routing.core.adapter;

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
 */
public class DocumentRouteAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
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
            return new GraphNodeImpl(doc);
        } else if (doc.hasFacet(DocumentRoutingConstants.ROUTE_STEP_FACET)) {
            return new DocumentRouteElementImpl(doc, new StepElementRunner());
        } else if (doc.hasFacet(DocumentRoutingConstants.CONDITIONAL_STEP_FACET)) {
            return new DocumentRouteStepsContainerImpl(doc, new ConditionalRunner());
        } else if (doc.hasFacet(DocumentRoutingConstants.STEP_FOLDER_FACET)) {
            ExecutionTypeValues executionType = getExecutionType(doc, type);
            switch (executionType) {
            case serial:
                return new DocumentRouteStepsContainerImpl(doc, new SerialRunner());
            case parallel:
                return new DocumentRouteStepsContainerImpl(doc, new ParallelRunner());
            case graph:
                throw new UnsupportedOperationException();
            }
        }
        return null;
    }

    protected ExecutionTypeValues getExecutionType(DocumentModel doc, String type) {
        ExecutionTypeValues executionType = ExecutionTypeValues.valueOf(
                (String) doc.getPropertyValue(DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME));
        return executionType;
    }

}
