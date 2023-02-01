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

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ROUTE_NODE_DOCUMENT_TYPE;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNodeImpl;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRouteImpl;

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
            return new GraphRouteImpl(doc);
        } else if (ROUTE_NODE_DOCUMENT_TYPE.equals(type)) {
            return new GraphNodeImpl(doc);
        }
        return null;
    }
}
