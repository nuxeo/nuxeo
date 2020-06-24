/*
 * (C) Copyright 2012-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;

public class TestDistributionResolver {

    protected static final String[] ALL_DISTRIBS = {
            // legacy
            "cap-5.5", "cap-5.5-RC1", "cap-5.5-SNAPSHOT", "cap-5.6-SNAPSHOT", "cap-5.6-RC1", "dm-5.5", "Nuxeo DM-5.4.1",
            "Nuxeo Platform-5.6-RC1", "server-10.10",
            // as of 11.1
            "server-11.1-SNAPSHOT", "server-11.1", "server-11.1.1", "server.11.1.1-SNAPSHOT",
            "server-11.1-I20200502_2326" };

    protected static final List<DistributionSnapshot> fakeSnapshots = new ArrayList<>();

    @BeforeClass
    public static void buildFakeSnapshots() {
        for (String id : ALL_DISTRIBS) {
            DistributionSnapshot snap = mock(DistributionSnapshot.class);
            when(snap.getKey()).thenReturn(id);
            String name = id.split("-")[0];
            when(snap.getName()).thenReturn(name);
            String version = id.replace(name + "-", "");
            when(snap.getVersion()).thenReturn(version);
            if (id.startsWith("server-11.1.1")) {
                when(snap.getAliases()).thenReturn(List.of(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT));
            } else {
                when(snap.getAliases()).thenReturn(List.of());
            }
            fakeSnapshots.add(snap);
        }
    }

    @AfterClass
    public static void clearFakeSnapshots() {
        fakeSnapshots.clear();
    }

    protected void check(String expected, String target) {
        String match = SnapshotResolverHelper.findBestMatch(fakeSnapshots, target);
        assertEquals(expected, match);
    }

    @Test
    public void testResolver() {
        check(null, "");
        // legacy
        check("Nuxeo Platform-5.6-RC1", "dm-5.6");
        check("dm-5.5", "dm-5.5");
        check("Nuxeo Platform-5.6-RC1", "cap-5.6");
        check("cap-5.6-SNAPSHOT", "cap-5.6-SNAPSHOT");
        check("cap-5.6-RC1", "cap-5.6-RC1");
        check("server-10.10", "server-10.10");
        // HF use case not covered
        check("server-11.1", "server-10.10-HF32");
        // as of 11.1
        check("server-11.1-SNAPSHOT", "server-11.1-SNAPSHOT");
        check("server-11.1", "server-11.1");
        check("server-11.1-I20200502_2326", "server-11.1-I20200502_2326"); // non-regression test for NXP-29193
        check("server-11.1-I20200502_2326", "server-11.1-I20200401_0001");
        check("server-11.1.1", "server-11.1.1-SNAPSHOT");
        check("server-11.1.1", "server-11.1.1");
        check("server-11.1.1", "server-11.1.2");
        check("server-11.1.1", "server-11.1.2-SNAPSHOT");
        check("server-11.1.1", "server-11.2-SNAPSHOT");
        check("server-11.1.1", SnapshotManager.DISTRIBUTION_ALIAS_CURRENT);
        check(null, "my-server-11.1-SNAPSHOT"); // non-regression test for NXP-29193
    }

}
