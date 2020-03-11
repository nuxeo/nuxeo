/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * All the tests of TestSQLBackend in no-path-optims mode, plus additional tests.
 */
public class TestSQLBackendNoPathOptims extends TestSQLBackend {

    protected boolean pathOptimizationsEnabled;

    @Override
    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        RepositoryDescriptor descriptor = super.newDescriptor(name, clusteringDelay);
        descriptor.setPathOptimizationsEnabled(pathOptimizationsEnabled);
        return descriptor;
    }

    @Test
    public void testPathOptimizationsActivation() throws Exception {
        Session session = repository.getConnection();
        PartialList<Serializable> res;
        Node root = session.getRootNode();
        List<Serializable> ids = new ArrayList<>();
        Node node = session.addChildNode(root, "r1", null, "TestDoc", false);
        for (int i = 0; i < 4; i++) {
            node = session.addChildNode(node, "node" + i, null, "TestDoc", false);
        }
        ids.add(node.getId()); // keep the latest
        session.save();
        List<Node> nodes = session.getNodesByIds(ids);
        assertEquals(1, nodes.size());
        String sql = "SELECT * FROM TestDoc WHERE ecm:path STARTSWITH '/r1'";
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(4, res.size());

        // reopen repository with path optimization to populate the ancestors table
        repository.close();

        pathOptimizationsEnabled  = true;
        repository = newRepository(-1);
        session = repository.getConnection();
        // this query will use nx_ancestors to bulk load the path
        nodes = session.getNodesByIds(ids);
        assertEquals(1, nodes.size());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(4, res.size());
    }

}
