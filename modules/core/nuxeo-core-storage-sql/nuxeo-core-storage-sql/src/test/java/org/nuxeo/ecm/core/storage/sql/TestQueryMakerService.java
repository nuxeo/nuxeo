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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerService;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.sql:OSGI-INF/querymaker-service.xml")
public class TestQueryMakerService {

    protected QueryMakerDescriptor desc;

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
        QueryMakerService queryMakerService = Framework.getService(QueryMakerService.class);
        QueryMakerDescriptor d;
        List<Class<? extends QueryMaker>> l;

        // first
        d = new QueryMakerDescriptor();
        d.name = "A";
        d.queryMaker = DummyQueryMaker1.class;
        queryMakerService.registerQueryMaker(d);
        l = queryMakerService.getQueryMakers();
        assertEquals(1, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));

        // second
        d = new QueryMakerDescriptor();
        d.name = "B";
        d.queryMaker = DummyQueryMaker2.class;
        queryMakerService.registerQueryMaker(d);
        l = queryMakerService.getQueryMakers();
        assertEquals(2, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));
        assertSame(DummyQueryMaker2.class, l.get(1));

        // disable first
        d = new QueryMakerDescriptor();
        d.name = "A";
        d.enabled = false;
        queryMakerService.registerQueryMaker(d);
        l = queryMakerService.getQueryMakers();
        assertEquals(1, l.size());
        assertSame(DummyQueryMaker2.class, l.get(0));

        // override second
        d = new QueryMakerDescriptor();
        d.name = "B";
        d.queryMaker = DummyQueryMaker1.class;
        queryMakerService.registerQueryMaker(d);
        l = queryMakerService.getQueryMakers();
        assertEquals(1, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));

        // add another of the first
        d = new QueryMakerDescriptor();
        d.name = "A";
        d.queryMaker = DummyQueryMaker2.class;
        queryMakerService.registerQueryMaker(d);
        l = queryMakerService.getQueryMakers();
        assertEquals(2, l.size());
        assertSame(DummyQueryMaker1.class, l.get(0));
        assertSame(DummyQueryMaker2.class, l.get(1));
    }

}
