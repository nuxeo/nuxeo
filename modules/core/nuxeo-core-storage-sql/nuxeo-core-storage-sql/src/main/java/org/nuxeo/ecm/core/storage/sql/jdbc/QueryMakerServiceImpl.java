/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service for the registration of QueryMaker classes.
 */
public class QueryMakerServiceImpl extends DefaultComponent implements QueryMakerService {

    private static final Log log = LogFactory.getLog(QueryMakerServiceImpl.class);

    public static final String XP = "queryMaker";

    protected final List<QueryMakerDescriptor> descriptors = new ArrayList<>(2);

    protected List<Class<? extends QueryMaker>> queryMakers;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP.equals(extensionPoint)) {
            registerQueryMaker((QueryMakerDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP.equals(extensionPoint)) {
            unregisterQueryMaker((QueryMakerDescriptor) contribution);
        }
    }

    @Override
    public void registerQueryMaker(QueryMakerDescriptor descriptor) {
        if (descriptor.enabled) {
            log.info(String.format("Registering QueryMaker '%s': %s", descriptor.name, descriptor.queryMaker.getName()));
        } else {
            log.info(String.format("Disabling QueryMaker '%s'", descriptor.name));
        }
        descriptors.add(descriptor);
        queryMakers = null;
    }

    @Override
    public void unregisterQueryMaker(QueryMakerDescriptor descriptor) {
        if (descriptor.enabled) {
            log.info(String.format("Unregistering QueryMaker '%s': %s", descriptor.name,
                    descriptor.queryMaker.getName()));
        } else {
            log.info(String.format("Unregistering disabled QueryMaker '%s'", descriptor.name));
        }
        descriptors.remove(descriptor);
        queryMakers = null;
    }

    @Override
    public synchronized List<Class<? extends QueryMaker>> getQueryMakers() {
        if (queryMakers == null) {
            // recompute queryMakers
            queryMakers = new ArrayList<>(2);
            List<QueryMakerDescriptor> qmdl = new ArrayList<>(descriptors);
            Collections.reverse(qmdl);
            Set<String> done = new HashSet<>();
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
            if (queryMakers.isEmpty() && !done.contains(NXQL.NXQL)) {
                queryMakers.add(NXQLQueryMaker.class);
            }
        }
        return queryMakers;
    }
}
