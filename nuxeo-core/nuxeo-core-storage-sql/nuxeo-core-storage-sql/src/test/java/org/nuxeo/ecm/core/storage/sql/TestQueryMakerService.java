/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerService;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestQueryMakerService extends NXRuntimeTestCase {

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
        public Query buildQuery(SQLInfo sqlInfo, Model model,
                PathResolver pathResolver, String query,
                QueryFilter queryFilter, Object... params)
                throws StorageException {
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
        public Query buildQuery(SQLInfo sqlInfo, Model model,
                PathResolver pathResolver, String query,
                QueryFilter queryFilter, Object... params)
                throws StorageException {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployTestContrib("org.nuxeo.ecm.core.storage.sql",
                "OSGI-INF/querymaker-service.xml");
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
