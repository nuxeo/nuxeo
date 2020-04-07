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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
@Deploy("org.nuxeo.apidoc.repo:apidoc-plugin-test-contrib.xml")
public class TestPlugin {

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void testRegistration() {
        Plugin<?> foo = snapshotManager.getPlugin("foo");
        assertNull(foo);
        Plugin<?> bar = snapshotManager.getPlugin("bar");
        assertNull(bar);
        Plugin<?> bat = snapshotManager.getPlugin("bat");
        assertNull(bat);

        List<String> errors = Framework.getRuntime().getMessageHandler().getErrors();
        assertNotNull(errors);
        assertEquals(2, errors.size());
        String error_foo = "Failed to register plugin with id 'foo' on 'org.nuxeo.apidoc.snapshot.SnapshotManagerComponent': "
                + "error initializing class 'org.nuxeo.apidoc.test.FooPlugin' "
                + "(java.lang.ClassNotFoundException: org.nuxeo.apidoc.test.FooPlugin).";
        assertEquals(error_foo, errors.get(0));
        String error_bar = "Failed to register plugin with id 'bar' on 'org.nuxeo.apidoc.snapshot.SnapshotManagerComponent': "
                + "error initializing class 'org.nuxeo.apidoc.test.FakeNuxeoArtifact' "
                + "(java.lang.NoSuchMethodException: org.nuxeo.apidoc.test.FakeNuxeoArtifact.<init>(org.nuxeo.apidoc.plugin.PluginDescriptor)).";
        assertEquals(error_bar, errors.get(1));
        Plugin<?> p = snapshotManager.getPlugin("testPlugin");
        assertNotNull(p);
    }

    @Test
    public void testPlugins() {
        List<Plugin<?>> plugins = snapshotManager.getPlugins();
        assertNotNull(plugins);
        assertEquals(1, plugins.size());
    }

    @Test
    public void testPlugin() {
        Plugin<?> p = snapshotManager.getPlugin("testPlugin");
        assertNotNull(p);
        assertEquals("testPlugin", p.getId());
        assertEquals("myType", p.getViewType());
        assertEquals("My snapshot plugin", p.getLabel());
        assertEquals("listItems", p.getHomeView());
        assertEquals("myStyleClass", p.getStyleClass());
        assertFalse(p.isHidden());
    }

    @Test
    public void testPluginRuntimeSnapshot() {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        PluginSnapshot<?> psnap = snapshot.getPluginSnapshots().get(FakePlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof FakePluginRuntimeSnapshot);
        FakePluginRuntimeSnapshot fpsnap = (FakePluginRuntimeSnapshot) psnap;
        List<String> itemIds = fpsnap.getItemIds();
        assertNotNull(itemIds);
        assertEquals(3, itemIds.size());
        assertEquals("org.nuxeo.apidoc.core", itemIds.get(0));
        assertEquals("org.nuxeo.apidoc.adapterContrib", itemIds.get(1));
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins", itemIds.get(2));

        // get introspected version from one of the bundles (e.g. 11.1-SNAPSHOT when writing this test)
        String version = snapshot.getBundle(snapshot.getBundleIds().get(0)).getArtifactVersion();

        FakeNuxeoArtifact item = fpsnap.getItem(itemIds.get(0));
        assertNotNull(item);
        assertEquals("org.nuxeo.apidoc.core", item.getId());
        assertEquals(BundleInfo.TYPE_NAME, item.getArtifactType());
        assertEquals(version, item.getVersion());

        item = fpsnap.getItem(itemIds.get(1));
        assertNotNull(item);
        assertEquals("org.nuxeo.apidoc.adapterContrib", item.getId());
        assertEquals(ComponentInfo.TYPE_NAME, item.getArtifactType());
        assertEquals(version, item.getVersion());

        item = fpsnap.getItem(itemIds.get(2));
        assertNotNull(item);
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins", item.getId());
        assertEquals(ExtensionPointInfo.TYPE_NAME, item.getArtifactType());
        assertEquals(version, item.getVersion());
    }

}
