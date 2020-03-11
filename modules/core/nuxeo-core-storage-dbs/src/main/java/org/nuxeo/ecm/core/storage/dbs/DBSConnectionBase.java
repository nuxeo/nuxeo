/*
 * (C) Copyright 2014-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.storage.State;

/**
 * Interface for a connection to a {@link DBSRepository}. The connection maintains state when it is transactional.
 *
 * @since 11.1
 */
public abstract class DBSConnectionBase implements DBSConnection {

    protected final DBSRepositoryBase repository;

    public DBSConnectionBase(DBSRepositoryBase repository) {
        this.repository = repository;
    }

    @Override
    public String getRootId() {
        if (DBSRepositoryBase.DEBUG_UUIDS) {
            return DBSRepositoryBase.UUID_ZERO_DEBUG;
        }
        switch (repository.getIdType()) {
        case varchar:
        case uuid:
            return DBSRepositoryBase.UUID_ZERO;
        case sequence:
            return "0";
        case sequenceHexRandomized:
            return "0000000000000000";
        default:
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Initializes the root and its ACP.
     */
    public void initRoot() {
        State state = new State();
        state.put(KEY_ID, getRootId());
        state.put(KEY_NAME, "");
        state.put(KEY_PRIMARY_TYPE, DBSRepositoryBase.TYPE_ROOT);
        state.put(KEY_ACP, DBSSession.acpToMem(getRootACP()));
        createState(state);
    }

    protected ACPImpl getRootACP() {
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.ADMINISTRATORS, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(SecurityConstants.MEMBERS, SecurityConstants.READ, true));
        ACPImpl acp = new ACPImpl();
        acp.addACL(acl);
        return acp;
    }

}
