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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * Integration Tests for NetBackend. Assumes a {@link NuxeoServerRunner} has
 * been started.
 */
public class ITSQLBackendNet extends TestSQLBackend {

    @Override
    public boolean initDatabase() {
        return false;
    }

    @Override
    public void tearDown() throws Exception {
        // make sure all connections were closed
        ((RepositoryImpl) repository).closeAllSessions();
        // delete all documents under the root
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        List<Node> children = session.getChildren(root, null, false);
        for (Node node : children) {
            session.removeNode(node);
        }
        // delete all the versions
        PartialList<Serializable> res = session.query(
                "SELECT * FROM Document WHERE ecm:isCheckedInVersion = 1",
                QueryFilter.EMPTY, false);
        for (Serializable id : res.list) {
            session.removeNode(session.getNodeById(id));
        }
        // reset ACLs as well
        CollectionProperty acls = root.getCollectionProperty(Model.ACL_PROP);
        ACLRow acl1 = new ACLRow(0, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATORS,
                null);
        ACLRow acl2 = new ACLRow(1, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATOR,
                null);
        ACLRow acl3 = new ACLRow(2, ACL.LOCAL_ACL, true,
                SecurityConstants.READ, SecurityConstants.MEMBERS, null);
        acls.setValue(new ACLRow[] { acl1, acl2, acl3 });
        session.save();
        session.close();
        super.tearDown();
    }

    @Override
    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay,
                fulltextDisabled);
        descriptor.name = "client";
        descriptor.binaryStorePath = "clientbinaries";
        ServerDescriptor sd = new ServerDescriptor();
        sd.host = "localhost";
        sd.port = 8181;
        sd.path = "/nuxeo";
        descriptor.connect = Collections.singletonList(sd);
        descriptor.binaryManagerConnect = true;
        return descriptor;
    }

}
