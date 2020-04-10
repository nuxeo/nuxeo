/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.plugin.AbstractPlugin;
import org.nuxeo.apidoc.plugin.PluginDescriptor;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @since 11.1
 */
public class FakePlugin extends AbstractPlugin<FakeNuxeoArtifact> {

    public static final String ID = "testPlugin";

    protected FakePluginRuntimeSnapshot runtimeSnapshot = new FakePluginRuntimeSnapshot(ID);

    @JsonCreator
    private FakePlugin(@JsonProperty("runtimeSnapshot") FakePluginRuntimeSnapshot runtimeSnapshot) {
        this((PluginDescriptor) null);
        this.runtimeSnapshot = runtimeSnapshot;
    }

    public FakePlugin(PluginDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public ObjectMapper enrishJsonMapper(ObjectMapper parent) {
        parent.registerModule(new SimpleModule().addAbstractTypeMapping(NuxeoArtifact.class, FakeNuxeoArtifact.class));
        return parent;
    }

    @Override
    public void persist(DistributionSnapshot snapshot, CoreSession session, DocumentModel root, SnapshotFilter filter) {
        // NOOP
    }

    @Override
    public PluginSnapshot<FakeNuxeoArtifact> getRuntimeSnapshot(DistributionSnapshot snapshot) {
        if (!runtimeSnapshot.initialized) {
            runtimeSnapshot.init(snapshot);
        }
        return runtimeSnapshot;
    }

    @Override
    public PluginSnapshot<FakeNuxeoArtifact> getRepositorySnapshot(DocumentModel root) {
        // NOOP
        return null;
    }

}
