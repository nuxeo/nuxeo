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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestSnapshotPersist {

    // helper for quicker update when running tests locally
    public static final boolean UPDATE_REFERENCE_FILES_ON_FAILURE = false;

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
        checkDistributionSnapshot(snapshot);
    }

    @Test
    public void testPersist() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(snapshot);
        checkDistributionSnapshot(snapshot);

        DistributionSnapshot persisted = snapshotManager.getSnapshot(snapshot.getKey(), session);
        assertNotNull(persisted);
        checkDistributionSnapshot(persisted);
    }

    protected void checkDistributionSnapshot(DistributionSnapshot snapshot) throws IOException {
        checkBundleGroups(snapshot);
        checkBundles(snapshot);
        checkComponents(snapshot);
        checkServices(snapshot);
        checkExtensionPoints(snapshot);
        checkContributions(snapshot);
        checkOperations(snapshot);
    }

    protected void checkBundleGroups(DistributionSnapshot snapshot) throws IOException {
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
        checkContentEquals("apidoc_snapshot/bundlegroups.txt", sb.toString());
    }

    protected String represent(NuxeoArtifact artifact) {
        return String.format("%s: %s *** %s\n", //
                artifact.getArtifactType(), //
                artifact.getId(), //
                artifact.getHierarchyPath() //
        );
    }

    protected void checkBundles(DistributionSnapshot snapshot) throws IOException {
        List<String> bids = snapshot.getBundleIds();
        Collections.sort(bids);

        StringBuilder sb = new StringBuilder();
        bids.forEach(bid -> sb.append(represent(snapshot.getBundle(bid))));

        checkContentEquals("apidoc_snapshot/bundles.txt", sb.toString());
    }

    protected void checkComponents(DistributionSnapshot snapshot) throws IOException {
        List<String> cids = snapshot.getComponentIds();
        Collections.sort(cids);

        StringBuilder sb = new StringBuilder();
        cids.forEach(cid -> sb.append(represent(snapshot.getComponent(cid))));

        checkContentEquals("apidoc_snapshot/components.txt", sb.toString());
    }

    protected void checkServices(DistributionSnapshot snapshot) throws IOException {
        List<String> sids = snapshot.getServiceIds();
        Collections.sort(sids);

        StringBuilder sb = new StringBuilder();
        sids.forEach(sid -> sb.append(represent(snapshot.getService(sid))));

        checkContentEquals("apidoc_snapshot/services.txt", sb.toString());
    }

    protected void checkExtensionPoints(DistributionSnapshot snapshot) throws IOException {
        List<String> epids = snapshot.getExtensionPointIds();
        Collections.sort(epids);

        StringBuilder sb = new StringBuilder();
        epids.forEach(epid -> sb.append(represent(snapshot.getExtensionPoint(epid))));

        checkContentEquals("apidoc_snapshot/extensionpoints.txt", sb.toString());
    }

    protected void checkContributions(DistributionSnapshot snapshot) throws IOException {
        List<String> exids = snapshot.getContributionIds();
        Collections.sort(exids);

        StringBuilder sb = new StringBuilder();
        exids.forEach(exid -> sb.append(represent(snapshot.getContribution(exid))));

        checkContentEquals("apidoc_snapshot/contributions.txt", sb.toString());
    }

    protected void checkOperations(DistributionSnapshot snapshot) throws IOException {
        List<OperationInfo> ops = snapshot.getOperations();
        Collections.sort(ops, Comparator.comparing(OperationInfo::getId));

        StringBuilder sb = new StringBuilder();
        ops.forEach(op -> sb.append(represent(op)));

        checkContentEquals("apidoc_snapshot/operations.txt", sb.toString());
    }

    protected void checkContentEquals(String path, String actualContent) throws IOException {
        String message = String.format("File '%s' content differs: ", path);
        String expectedPath = getReferencePath(path);
        String expectedContent = getReferenceContent(expectedPath);
        if (actualContent != null) {
            actualContent = actualContent.trim();
            if (SystemUtils.IS_OS_WINDOWS) {
                // replace end of lines while testing on windows
                actualContent = actualContent.replaceAll("\r?\n", "\n");
            }
        }
        try {
            assertEquals(message, expectedContent, actualContent);
        } catch (ComparisonFailure e) {
            // copy content locally to ease up updates when running tests locally
            if (UPDATE_REFERENCE_FILES_ON_FAILURE) {
                // ugly hack to get the actual resource file path:
                // - bin/* are for Eclipse;
                // - target/classes* for IntelliJ.
                String resourcePath = expectedPath.replace("bin/test", "src/test/resources")
                                                  .replace("bin/main", "src/main/resources")
                                                  .replace("target/test-classes", "src/test/resources")
                                                  .replace("target/classes", "src/main/resources");
                org.apache.commons.io.FileUtils.copyInputStreamToFile(
                        new ByteArrayInputStream(actualContent.getBytes()), new File(resourcePath));
            }
            throw e;
        }
    }

    public static String getReferencePath(String path) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + path);
        }
        return FileUtils.getFilePathFromUrl(fileUrl);
    }

    public static String getReferenceContent(String refPath) throws IOException {
        return org.apache.commons.io.FileUtils.readFileToString(new File(refPath), StandardCharsets.UTF_8).trim();
    }
}
