/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.pubsub.AbstractPubSubInvalidator;

/**
 * PubSub implementation of {@link DBSClusterInvalidator}.
 *
 * @since 9.1
 */
public class DBSPubSubInvalidator extends AbstractPubSubInvalidator<DBSInvalidations> implements DBSClusterInvalidator {

    @Override
    public DBSInvalidations newInvalidations() {
        return new DBSInvalidations();
    }

    @Override
    public DBSInvalidations deserialize(InputStream in) throws IOException {
        return DBSInvalidations.deserialize(in);
    }

    @Override
    public void initialize(String nodeId, String repositoryName) {
        super.initialize("dbs:" + repositoryName, nodeId);
    }

}
