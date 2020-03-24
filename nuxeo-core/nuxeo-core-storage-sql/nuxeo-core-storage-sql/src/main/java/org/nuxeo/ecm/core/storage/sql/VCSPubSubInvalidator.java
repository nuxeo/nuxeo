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
package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.runtime.pubsub.AbstractPubSubInvalidationsAccumulator;

/**
 * PubSub implementation of the VCS {@link VCSClusterInvalidator}.
 *
 * @since 9.1
 */
public class VCSPubSubInvalidator extends AbstractPubSubInvalidationsAccumulator<VCSInvalidations> implements VCSClusterInvalidator {

    @Override
    public VCSInvalidations newInvalidations() {
        return new VCSInvalidations();
    }

    @Override
    public void initialize(String nodeId, RepositoryImpl repository) {
        initialize("vcs:" + repository.getName(), nodeId);
    }

    @Override
    public VCSInvalidations deserialize(InputStream in) throws IOException {
        return VCSInvalidations.deserialize(in);
    }

}
