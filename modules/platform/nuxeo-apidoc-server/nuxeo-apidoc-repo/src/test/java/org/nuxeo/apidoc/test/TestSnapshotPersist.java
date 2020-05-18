/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestSnapshotPersist extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Inject
    protected CoreFeature coreFeature;

    @Before
    public void checkIsVCSH2() {
        assumeTrue(coreFeature.getStorageConfiguration().isVCSH2());
    }

    @Test
    public void testSnapshot() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        checkDistributionSnapshot(snapshot, false);
    }

    @Test
    public void testPersist() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(snapshot);
        checkDistributionSnapshot(snapshot, false);

        DistributionSnapshot persisted = snapshotManager.getSnapshot(snapshot.getKey(), session);
        assertNotNull(persisted);
        checkDistributionSnapshot(persisted, false);
    }

    @Test
    public void testPersistPartial() throws IOException {
        SnapshotFilter filter = new SnapshotFilter("apidoc");
        filter.addBundlePrefix("org.nuxeo.apidoc");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);
        checkDistributionSnapshot(snapshot, true);

        DistributionSnapshot persisted = snapshotManager.getSnapshot(snapshot.getKey(), session);
        assertNotNull(persisted);
        checkDistributionSnapshot(persisted, true);
    }

    protected void checkDistributionSnapshot(DistributionSnapshot snapshot, boolean partial) throws IOException {
        checkBundleGroups(snapshot, partial);
        checkBundles(snapshot, partial);
        checkComponents(snapshot, partial);
        checkServices(snapshot, partial);
        checkExtensionPoints(snapshot, partial);
        checkContributions(snapshot, partial);
        checkOperations(snapshot, partial);
        checkReadmes(snapshot, partial);
    }

    protected void checkBundleGroups(DistributionSnapshot snapshot, boolean partial) throws IOException {
        StringBuilder sb = new StringBuilder();
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(snapshot);
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            sb.append(String.format("%s- %s (%s) *** %s\n", //
                    StringUtils.repeat("  ", info.getLevel()), //
                    info.getGroup().getName(), //
                    info.getGroup().getId(), //
                    info.getGroup().getHierarchyPath()) //
            );
        }
        if (partial) {
            // only one virtual bundle group is created in case of partial persistence
            assertEquals("- apidoc (apidoc) *** /apidoc\n", sb.toString());
        } else {
            checkContentEquals("apidoc_snapshot/bundlegroups.txt", sb.toString());
        }
    }

    protected String represent(NuxeoArtifact artifact) {
        List<String> requirements = null;
        if (artifact instanceof BundleInfo) {
            requirements = ((BundleInfo) artifact).getRequirements();
        }
        if (artifact instanceof ComponentInfo) {
            requirements = ((ComponentInfo) artifact).getRequirements();
        }
        String res = String.format("%s: %s *** %s%s\n", //
                artifact.getArtifactType(), //
                artifact.getId(), //
                artifact.getHierarchyPath(), //
                (requirements != null && requirements.size() > 0) ? " *** " + requirements : ""//
        );
        return res;
    }

    protected void checkBundles(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<String> bids = snapshot.getBundleIds();
        String s = bids.stream().map(snapshot::getBundle).map(this::represent).collect(Collectors.joining());
        if (partial) {
            checkContentEquals("apidoc_snapshot/bundles_partial.txt", s);
        } else {
            checkContentEquals("apidoc_snapshot/bundles.txt", s);
        }
    }

    protected void checkComponents(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<String> cids = snapshot.getComponentIds();
        String s = cids.stream().map(snapshot::getComponent).map(this::represent).collect(Collectors.joining());
        if (partial) {
            checkContentEquals("apidoc_snapshot/components_partial.txt", s);
        } else {
            checkContentEquals("apidoc_snapshot/components.txt", s);
        }
    }

    protected void checkServices(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<String> sids = snapshot.getServiceIds();
        String s = sids.stream().map(snapshot::getService).map(this::represent).collect(Collectors.joining());
        if (partial) {
            checkContentEquals("apidoc_snapshot/services_partial.txt", s);
        } else {
            checkContentEquals("apidoc_snapshot/services.txt", s);
        }
    }

    protected void checkExtensionPoints(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<String> epids = snapshot.getExtensionPointIds();
        String s = epids.stream().map(snapshot::getExtensionPoint).map(this::represent).collect(Collectors.joining());
        if (partial) {
            checkContentEquals("apidoc_snapshot/extensionpoints_partial.txt", s);
        } else {
            checkContentEquals("apidoc_snapshot/extensionpoints.txt", s);
        }
    }

    protected void checkContributions(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<String> exids = snapshot.getContributionIds();
        String s = exids.stream().map(snapshot::getContribution).map(this::represent).collect(Collectors.joining());
        if (partial) {
            checkContentEquals("apidoc_snapshot/contributions_partial.txt", s);
        } else {
            checkContentEquals("apidoc_snapshot/contributions.txt", s);
        }
    }

    protected void checkOperations(DistributionSnapshot snapshot, boolean partial) throws IOException {
        List<OperationInfo> ops = snapshot.getOperations();
        String s = ops.stream().map(this::represent).collect(Collectors.joining());
        if (partial) {
            // no operations in apidoc modules
            assertEquals("", s);
        } else {
            checkContentEquals("apidoc_snapshot/operations.txt", s);
        }
    }

    protected void checkReadmes(DistributionSnapshot snapshot, boolean partial) throws IOException {
        BundleInfo bundle = snapshot.getBundle("org.nuxeo.apidoc.core");
        assertNotNull(bundle);

        Blob readme = bundle.getReadme();
        assertNotNull(readme);
        checkContentEquals("apidoc_snapshot/core_readme.txt", readme.getString());

        Blob parentReadme = bundle.getParentReadme();
        assertNotNull(parentReadme);
        checkContentEquals("apidoc_snapshot/apidoc_readme.txt", parentReadme.getString());

        if (!partial) {
            BundleGroup bundleGroup = snapshot.getBundleGroup(bundle.getGroupId());
            assertNotNull(bundleGroup);
            assertNotNull(bundleGroup.getReadmes());
            assertEquals(1, bundleGroup.getReadmes().size());
            checkContentEquals("apidoc_snapshot/apidoc_readme.txt", bundleGroup.getReadmes().get(0).getString());
        }
    }

}
