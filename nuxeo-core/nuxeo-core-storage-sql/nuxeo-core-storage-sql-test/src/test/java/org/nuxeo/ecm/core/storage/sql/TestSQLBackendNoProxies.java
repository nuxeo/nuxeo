/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * All the tests of TestSQLBackend in no-proxies mode, plus additional tests.
 */
public class TestSQLBackendNoProxies extends TestSQLBackend {

    protected boolean proxiesEnabled = false;

    @Override
    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        RepositoryDescriptor descriptor = super.newDescriptor(name, clusteringDelay);
        descriptor.setProxiesEnabled(proxiesEnabled);
        return descriptor;
    }

    @Test
    public void testCreationDenied() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node doc = session.addChildNode(root, "doc", null, "TestDoc", false);
        Node ver = session.checkIn(doc, "v1", "");
        try {
            session.addProxy(ver.getId(), doc.getId(), root, "proxy", null);
            fail("Proxy creation should be denied");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Proxies are disabled by configuration"));
        }
        try {
            session.setProxyTarget(doc, doc.getId());
            fail("Proxy creation should be denied");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Proxies are disabled by configuration"));
        }
    }

    @Test
    public void testQueryReturnsNoProxies() throws Exception {
        // create proxy through repo 2, which allows proxies
        // second repo with proxies allowed
        proxiesEnabled = true;
        repository2 = newRepository("repo2", -1);
        proxiesEnabled = false;
        Session session2 = repository2.getConnection();
        Node root2 = session2.getRootNode();
        Node doc2 = session2.addChildNode(root2, "doc", null, "TestDoc", false);
        Node ver2 = session2.checkIn(doc2, "v1", "");
        session2.addProxy(ver2.getId(), doc2.getId(), root2, "proxy", null);
        session2.save();
        String sql = "SELECT * FROM Document WHERE ecm:name = 'proxy'";
        IterableQueryResult res2 = session2.queryAndFetch(sql, "NXQL", QueryFilter.EMPTY);
        try {
            assertEquals(1, res2.size());
        } finally {
            res2.close();
        }
        session2.close();

        // now in repository viewed without proxies
        Session session = repository.getConnection();
        // same query should return no proxy
        IterableQueryResult res = session.queryAndFetch(sql, "NXQL", QueryFilter.EMPTY);
        try {
            assertEquals(0, res.size());
        } finally {
            res.close();
        }
    }

    @Test
    public void testQueryOnlyProxiesDenied() throws Exception {
        Session session = repository.getConnection();
        String sql = "SELECT * FROM Document WHERE ecm:isProxy = 1";
        IterableQueryResult res = session.queryAndFetch(sql, "NXQL", QueryFilter.EMPTY);
        try {
            assertEquals(0, res.size());
        } finally {
            res.close();
        }
    }

}
