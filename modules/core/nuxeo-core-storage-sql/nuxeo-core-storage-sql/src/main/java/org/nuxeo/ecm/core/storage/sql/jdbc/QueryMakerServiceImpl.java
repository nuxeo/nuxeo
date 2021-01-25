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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service for the registration of QueryMaker classes.
 */
public class QueryMakerServiceImpl extends DefaultComponent implements QueryMakerService {

    public static final String XP = "queryMaker";

    protected List<Class<? extends QueryMaker>> queryMakers;

    @Override
    public void start(ComponentContext context) {
        queryMakers = this.<QueryMakerDescriptor> getRegistryContributions(XP)
                          .stream()
                          .map(QueryMakerDescriptor::getQueryMaker)
                          .collect(Collectors.toList());
        // backward compat
        if (queryMakers.isEmpty() || getRegistryContribution(XP, NXQL.NXQL).isEmpty()) {
            queryMakers.add(NXQLQueryMaker.class);
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        queryMakers = null;
    }

    @Override
    public List<Class<? extends QueryMaker>> getQueryMakers() {
        return Collections.unmodifiableList(queryMakers);
    }

}
