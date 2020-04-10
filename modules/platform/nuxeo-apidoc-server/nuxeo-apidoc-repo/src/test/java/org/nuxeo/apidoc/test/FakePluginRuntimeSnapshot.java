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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.plugin.AbstractPluginSnapshot;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 11.1
 */
public class FakePluginRuntimeSnapshot extends AbstractPluginSnapshot<FakeNuxeoArtifact>
        implements PluginSnapshot<FakeNuxeoArtifact> {

    protected boolean initialized = false;

    protected Map<String, FakeNuxeoArtifact> items = new LinkedHashMap<>();

    public FakePluginRuntimeSnapshot(String pluginId) {
        super(pluginId);
    }

    @JsonCreator
    private FakePluginRuntimeSnapshot(@JsonProperty("pluginId") String pluginId,
            @JsonProperty("items") List<FakeNuxeoArtifact> items) {
        this(pluginId);
        items.forEach(item -> this.items.put(item.getId(), item));
    }

    public void init(DistributionSnapshot snapshot) {
        if (initialized) {
            return;
        }
        // select items that we expect to be present
        String bid = "org.nuxeo.apidoc.core";
        items.put(bid, new FakeNuxeoArtifact(snapshot.getBundle(bid)));
        String cid = "org.nuxeo.apidoc.adapterContrib";
        items.put(cid, new FakeNuxeoArtifact(snapshot.getComponent(cid)));
        String epid = "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins";
        items.put(epid, new FakeNuxeoArtifact(snapshot.getExtensionPoint(epid)));
        initialized = true;
    }

    @Override
    public List<String> getItemIds() {
        return new ArrayList<>(items.keySet());
    }

    @Override
    public List<FakeNuxeoArtifact> getItems() {
        return new ArrayList<>(items.values());
    }

    @Override
    public FakeNuxeoArtifact getItem(String id) {
        return items.get(id);
    }

}
