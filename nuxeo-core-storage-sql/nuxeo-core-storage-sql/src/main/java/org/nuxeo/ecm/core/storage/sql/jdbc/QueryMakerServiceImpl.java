/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service for the registration of QueryMaker classes.
 */
public class QueryMakerServiceImpl extends DefaultComponent implements
        QueryMakerService {

    private static final Log log = LogFactory.getLog(QueryMakerServiceImpl.class);

    public static final String XP = "queryMaker";

    protected final List<QueryMakerDescriptor> descriptors = new ArrayList<QueryMakerDescriptor>(
            2);

    protected List<Class<? extends QueryMaker>> queryMakers;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP.equals(extensionPoint)) {
            registerQueryMaker((QueryMakerDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP.equals(extensionPoint)) {
            unregisterQueryMaker((QueryMakerDescriptor) contribution);
        }
    }

    public void registerQueryMaker(QueryMakerDescriptor descriptor) {
        if (descriptor.enabled) {
            log.info(String.format("Registering QueryMaker '%s': %s",
                    descriptor.name, descriptor.queryMaker.getName()));
        } else {
            log.info(String.format("Disabling QueryMaker '%s'", descriptor.name));
        }
        descriptors.add(descriptor);
        queryMakers = null;
    }

    public void unregisterQueryMaker(QueryMakerDescriptor descriptor) {
        if (descriptor.enabled) {
            log.info(String.format("Unregistering QueryMaker '%s': %s",
                    descriptor.name, descriptor.queryMaker.getName()));
        } else {
            log.info(String.format("Unregistering disabled QueryMaker '%s'",
                    descriptor.name));
        }
        descriptors.remove(descriptor);
        queryMakers = null;
    }

    public synchronized List<Class<? extends QueryMaker>> getQueryMakers() {
        if (queryMakers == null) {
            // recompute queryMakers
            queryMakers = new ArrayList<Class<? extends QueryMaker>>(2);
            List<QueryMakerDescriptor> qmdl = new ArrayList<QueryMakerDescriptor>(
                    descriptors);
            Collections.reverse(qmdl);
            Set<String> done = new HashSet<String>();
            for (QueryMakerDescriptor descriptor : qmdl) {
                if (!done.add(descriptor.name)) {
                    continue;
                }
                if (descriptor.enabled) {
                    queryMakers.add(descriptor.queryMaker);
                }
            }
            Collections.reverse(queryMakers);
            // BBB backward compat
            if (queryMakers.isEmpty() && !done.contains("NXQL")) {
                queryMakers.add(NXQLQueryMaker.class);
            }
        }
        return queryMakers;
    }
}
