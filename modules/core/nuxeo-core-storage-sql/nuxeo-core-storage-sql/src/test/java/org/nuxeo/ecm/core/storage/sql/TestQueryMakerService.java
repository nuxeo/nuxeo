/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerService;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.sql:OSGI-INF/querymaker-service.xml")
@Deploy("org.nuxeo.ecm.core.storage.sql.tests:test-querymaker-contrib.xml")
public class TestQueryMakerService {

    @Inject
    protected QueryMakerService queryMakerService;

    @Inject
    protected HotDeployer hotDeployer;

    public static class DummyQueryMaker1 implements QueryMaker {
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean accepts(String query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String query,
                QueryFilter queryFilter, Object... params) {
            throw new UnsupportedOperationException();
        }
    }

    public static class DummyQueryMaker2 implements QueryMaker {
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean accepts(String query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String query,
                QueryFilter queryFilter, Object... params) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testBasic() throws Exception {
        List<Class<? extends QueryMaker>> l;

        l = queryMakerService.getQueryMakers();
        assertEquals(3, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));
        assertSame(DummyQueryMaker2.class, l.get(1));
        assertSame(NXQLQueryMaker.class, l.get(2));

        hotDeployer.deploy("org.nuxeo.ecm.core.storage.sql.tests:test-querymaker-override.xml");

        // disabled first, overridden second
        l = queryMakerService.getQueryMakers();
        assertEquals(2, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));
        assertSame(NXQLQueryMaker.class, l.get(1));

        hotDeployer.deploy("org.nuxeo.ecm.core.storage.sql.tests:test-querymaker-override2.xml");

        // add another of the first
        l = queryMakerService.getQueryMakers();
        assertEquals(3, l.size());
        // A is still the first one
        assertSame(DummyQueryMaker2.class, l.get(0));
        assertSame(DummyQueryMaker1.class, l.get(1));
        assertSame(NXQLQueryMaker.class, l.get(2));
    }

}
