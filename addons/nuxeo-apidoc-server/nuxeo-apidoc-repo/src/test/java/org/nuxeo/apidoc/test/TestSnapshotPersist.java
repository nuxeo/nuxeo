/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.base.Charsets;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeSnaphotFeature.class })
public class TestSnapshotPersist {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    protected String dumpSnapshot(DistributionSnapshot snap) {
        StringBuilder sb = new StringBuilder();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(snap);
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad = " ";
            for (int i = 0; i <= info.getLevel(); i++) {
                pad += " ";
            }
            sb.append(pad)
              .append("- ")
              .append(info.getGroup().getName())
              .append("(")
              .append(info.getGroup().getId())
              .append(")");
            sb.append(" *** ");
            sb.append(info.getGroup().getHierarchyPath());
            sb.append("\n");
        }

        List<String> bids = snap.getBundleIds();
        List<String> cids = snap.getComponentIds();
        List<String> sids = snap.getServiceIds();
        List<String> epids = snap.getExtensionPointIds();
        List<String> exids = snap.getContributionIds();

        List<Graph> graphs = snap.getGraphs();

        for (String bid : bids) {
            sb.append("bundle: ").append(bid);
            BundleInfo bi = snap.getBundle(bid);
            sb.append(" *** ");
            sb.append(bi.getHierarchyPath());
            sb.append(" *** ");
            sb.append(bi.getRequirements());
            sb.append("\n");
        }

        for (String cid : cids) {
            sb.append("component: ").append(cid);
            sb.append(" *** ");
            ComponentInfo ci = snap.getComponent(cid);
            sb.append(ci.getHierarchyPath());
            sb.append(" *** ");
            sb.append(ci.getRequirements());
            sb.append("\n");
        }

        for (String sid : sids) {
            sb.append("service: ").append(sid);
            sb.append(" *** ");
            ServiceInfo si = snap.getService(sid);
            sb.append(si.getHierarchyPath());
            sb.append("\n");
        }

        for (String epid : epids) {
            sb.append("extensionPoint: ").append(epid);
            sb.append(" *** ");
            ExtensionPointInfo epi = snap.getExtensionPoint(epid);
            sb.append(epi.getHierarchyPath());
            sb.append("\n");
        }

        for (String exid : exids) {
            sb.append("contribution: ").append(exid);
            sb.append(" *** ");
            ExtensionInfo exi = snap.getContribution(exid);
            sb.append(exi.getHierarchyPath());
            sb.append("\n");
        }

        for (Graph graph : graphs) {
            sb.append("graph: ").append(graph.getName());
            sb.append(" *** ");
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    @Test
    public void testPersist() throws Exception {
        DistributionSnapshot runtimeSnapshot = snapshotManager.getRuntimeSnapshot();
        String rtDump = dumpSnapshot(runtimeSnapshot);

        DistributionSnapshot persistent = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(persistent);
        persistent = snapshotManager.getSnapshot(runtimeSnapshot.getKey(), session);
        assertNotNull(persistent);

        String pDump = dumpSnapshot(persistent);

        // String ref = "ref_dump.txt";
        // checkContentEquals(getReferenceFileContent(ref), rtDump, String.format("File '%s' content differs: ", ref));
        // checkContentEquals(getReferenceFileContent(ref), pDump, String.format("File '%s' content differs: ", ref));
        assertEquals(rtDump, pDump);

        // check runtime graph export equals to persistent *and* to reference graph
        List<Graph> runtimeGraphs = runtimeSnapshot.getGraphs();
        List<Graph> persistentGraphs = persistent.getGraphs();
        List<String> refs = new ArrayList<>();
        refs.add("basic_graph.json");

        assertEquals(runtimeGraphs.size(), persistentGraphs.size());
        assertEquals(runtimeGraphs.size(), refs.size());
        int i = 0;
        for (Graph graph : runtimeGraphs) {
            checkContentEquals(getReferenceFileContent(refs.get(i)), graph.getContent(),
                    String.format("File '%s' content differs: ", refs.get(i)));
            checkContentEquals(persistentGraphs.get(i).getContent(), graph.getContent(), null);
            i++;
        }
    }

    @Test
    public void testExportImport() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        File tempFile = Framework.createTempFile("testExportImport", snapshot.getKey());
        try (OutputStream out = new FileOutputStream(tempFile)) {
            snapshotManager.exportSnapshot(session, snapshot.getKey(), out);
        }
        try (InputStream in = new FileInputStream(tempFile)) {
            DocumentModel doc = snapshotManager.importTmpSnapshot(session, in);
            assertNotNull(doc);
        }
        tempFile.delete();
    }

    public static void checkFileEquals(String expectedPath, String actualPath) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(expectedPath);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + expectedPath);
        }
        String filePath = FileUtils.getFilePathFromUrl(fileUrl);

        String expectedString = org.apache.commons.io.FileUtils.readFileToString(new File(filePath), Charsets.UTF_8)
                                                               .trim();
        String actualString = org.apache.commons.io.FileUtils.readFileToString(new File(actualPath), Charsets.UTF_8)
                                                             .trim();
        checkContentEquals(expectedString, actualString,
                String.format("Files '%s' and '%s' differ: ", expectedPath, actualPath));
    }

    public static String getReferenceFileContent(String expectedPath) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(expectedPath);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + expectedPath);
        }
        String filePath = FileUtils.getFilePathFromUrl(fileUrl);

        String expectedString = org.apache.commons.io.FileUtils.readFileToString(new File(filePath), Charsets.UTF_8)
                                                               .trim();
        return expectedString;
    }

    public static void checkContentEquals(String expectedString, String actualString, String message)
            throws IOException {
        // replace end of lines while testing on windows
        if (SystemUtils.IS_OS_WINDOWS) {
            actualString = actualString.replaceAll("\r?\n", "\n");
        }

        try {
            assertEquals(message, expectedString, actualString);
        } catch (ComparisonFailure e) {
            throw e;
        }
    }

}