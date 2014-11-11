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
import java.util.List;

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

    public final List<Class<? extends QueryMaker>> queryMakers = new ArrayList<Class<? extends QueryMaker>>(
            2);

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP.equals(extensionPoint)) {
            QueryMakerDescriptor descriptor = (QueryMakerDescriptor) contribution;
            registerQueryMaker(descriptor.queryMaker);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP.equals(extensionPoint)) {
            QueryMakerDescriptor descriptor = (QueryMakerDescriptor) contribution;
            unregisterQueryMaker(descriptor.queryMaker);
        }
    }

    public void registerQueryMaker(Class<? extends QueryMaker> klass) {
        log.info("Registering QueryMaker: " + klass.getName());
        if (!queryMakers.contains(klass)) {
            queryMakers.add(klass);
        }
    }

    public void unregisterQueryMaker(Class<? extends QueryMaker> klass) {
        log.info("Unregistering QueryMaker: " + klass.getName());
        queryMakers.remove(klass);
    }

    public List<Class<? extends QueryMaker>> getQueryMakers() {
        if (queryMakers.isEmpty()) {
            return Collections.<Class<? extends QueryMaker>> singletonList(NXQLQueryMaker.class);
        } else {
            return queryMakers;
        }
    }

}
