/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.client.querymodel;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class QueryModelService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService";

    private static final Log log = LogFactory.getLog(QueryModelService.class);

    private Map<String, QueryModelDescriptor> descriptors;

    public QueryModelDescriptor getQueryModelDescriptor(String descriptorName) {
        return descriptors.get(descriptorName);
    }

    @Override
    public void activate(ComponentContext context) {
        descriptors = new HashMap<String, QueryModelDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        descriptors = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        QueryModelDescriptor descriptor = (QueryModelDescriptor) contribution;
        if (descriptor.isStateful()) {
            descriptor.initEscaper(contributor.getContext());
        }
        descriptors.put(descriptor.getName(), descriptor);
        log.debug("registered QueryModelDescriptor: "
                + descriptor.getName());
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        QueryModelDescriptor descriptor = (QueryModelDescriptor) contribution;
        descriptors.remove(descriptor.getName());
        log.debug("unregistered QueryModelDescriptor: "
                + descriptor.getName());
    }

}
