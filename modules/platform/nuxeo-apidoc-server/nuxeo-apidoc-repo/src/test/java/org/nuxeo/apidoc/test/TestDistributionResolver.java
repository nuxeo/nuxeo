/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDistributionResolver {

    protected static final String[] alldistribs = { "cap-5.5", "cap-5.5-RC1", "cap-5.5-SNAPSHOT", "cap-5.6-SNAPSHOT",
            "cap-5.6-RC1", "dm-5.5", "Nuxeo DM-5.4.1", "Nuxeo Platform-5.6-RC1" };

    protected List<DistributionSnapshot> buildFakeSnaps() {

        List<DistributionSnapshot> snaps = new ArrayList<>();
        for (final String distId : alldistribs) {

            DistributionSnapshot snap = new DistributionSnapshot() {

                String id = distId;

                @Override
                public boolean isLive() {
                    return false;
                }

                @Override
                public String getVersion() {
                    return id.replace(getName() + "-", "");
                }

                @Override
                public String getName() {
                    return distId.split("-")[0];
                }

                @Override
                public Date getCreationDate() {
                    return null;
                }

                @Override
                public Date getReleaseDate() {
                    return null;
                }

                @Override
                public List<String> getXmlComponentIds() {
                    return null;
                }

                @Override
                public List<String> getServiceIds() {
                    return null;
                }

                @Override
                public ServiceInfo getService(String id) {
                    return null;
                }

                @Override
                public List<OperationInfo> getOperations() {
                    return null;
                }

                @Override
                public OperationInfo getOperation(String id) {
                    return null;
                }

                @Override
                public String getKey() {
                    return id;
                }

                @Override
                public void cleanPreviousArtifacts() {
                    // empty body
                }

                @Override
                public boolean isLatestFT() {
                    return false;
                }

                @Override
                public boolean isLatestLTS() {
                    return false;
                }

                @Override
                public List<String> getAliases() {
                    return Collections.emptyList();
                }

                @Override
                public boolean isHidden() {
                    return false;
                }

                @Override
                public List<String> getJavaComponentIds() {
                    return null;
                }

                @Override
                public List<String> getExtensionPointIds() {
                    return null;
                }

                @Override
                public ExtensionPointInfo getExtensionPoint(String id) {
                    return null;
                }

                @Override
                public List<ExtensionInfo> getContributions() {
                    return null;
                }

                @Override
                public List<String> getContributionIds() {
                    return null;
                }

                @Override
                public ExtensionInfo getContribution(String id) {
                    return null;
                }

                @Override
                public List<String> getComponentIds() {
                    return null;
                }

                @Override
                public ComponentInfo getComponent(String id) {
                    return null;
                }

                @Override
                public List<String> getBundleIds() {
                    return null;
                }

                @Override
                public List<BundleInfo> getBundles() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<BundleGroup> getBundleGroups() {
                    return null;
                }

                @Override
                public List<String> getBundleGroupChildren(String groupId) {
                    return null;
                }

                @Override
                public BundleGroup getBundleGroup(String groupId) {
                    return null;
                }

                @Override
                public BundleInfo getBundle(String id) {
                    return null;
                }

                @Override
                public ObjectMapper getJsonMapper() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void writeJson(OutputStream out) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DistributionSnapshot readJson(InputStream in) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
                    throw new UnsupportedOperationException();
                }

            };
            snaps.add(snap);
        }

        return snaps;
    }

    protected String testResolution(List<DistributionSnapshot> snaps, String target) {
        String match = SnapshotResolverHelper.findBestMatch(snaps, target);
        assertNotNull(match);
        return match;
    }

    @Test
    public void testResolver() {
        List<DistributionSnapshot> snaps = buildFakeSnaps();
        String res = testResolution(snaps, "dm-5.6");
        assertEquals("Nuxeo Platform-5.6-RC1", res);
        res = testResolution(snaps, "dm-5.5");
        assertEquals("dm-5.5", res);
        res = testResolution(snaps, "cap-5.6");
        assertEquals("Nuxeo Platform-5.6-RC1", res);
        res = testResolution(snaps, "cap-5.6-SNAPSHOT");
        assertEquals("cap-5.6-SNAPSHOT", res);
        res = testResolution(snaps, "cap-5.6-RC1");
        assertEquals("cap-5.6-RC1", res);

    }
}
