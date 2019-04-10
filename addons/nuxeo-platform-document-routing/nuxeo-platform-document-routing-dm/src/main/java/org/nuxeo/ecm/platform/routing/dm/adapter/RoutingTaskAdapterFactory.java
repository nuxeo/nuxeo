/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 *
 * @author ldoguin
 */
public class RoutingTaskAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc,
            @SuppressWarnings("rawtypes") Class itf) {
        if (doc.hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME)) {
            return new RoutingTaskImpl(doc);
        }
        return null;
    }

}
