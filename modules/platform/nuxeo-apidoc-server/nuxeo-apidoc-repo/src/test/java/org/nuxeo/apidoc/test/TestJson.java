/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonPrettyPrinter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestJson extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void canSerializeRuntimeAndReadBack() throws IOException {
        DistributionSnapshot snapshot = RuntimeSnapshot.build();
        assertNotNull(snapshot);
        canSerializeAndReadBack(snapshot);
    }

    @Test
    public void cannotSerializeRepositoryAndReadBack() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(snapshot);
        try {
            canSerializeAndReadBack(snapshot);
            fail("should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

    protected void canSerializeAndReadBack(DistributionSnapshot snap) throws IOException {
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            snap.writeJson(sink);
            try (OutputStream file = Files.newOutputStream(Paths.get(FeaturesRunner.getBuildDirectory() + "/test.json"),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                file.write(sink.toByteArray());
            }
            try (ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray())) {
                DistributionSnapshot snapshot = snap.readJson(source);
                assertNotNull(snapshot);
                assertNotNull(snapshot.getBundle("org.nuxeo.apidoc.repo"));
            }
        }
    }

    @Ignore("Only useful to update reference test-export.json file")
    @Test
    public void doWriteLegacy() throws IOException {
        RuntimeSnapshot snapshot = RuntimeSnapshot.build();
        assertNotNull(snapshot);

        class ReferenceRuntime extends RuntimeSnapshot {
            RuntimeSnapshot orig;

            public ReferenceRuntime(RuntimeSnapshot orig) {
                this.orig = orig;
            }

            @Override
            public List<OperationInfo> getOperations() {
                return orig.getOperations().subList(0, 2);
            }

            @Override
            public List<BundleInfo> getBundles() {
                return orig.getBundles().subList(0, 2);
            }

            @Override
            protected void initOperations() {
                // NOOP
            }

            @Override
            public void writeJson(OutputStream out) {
                writeJson(out, new JsonPrettyPrinter());
            }

            @Override
            public String getVersion() {
                return orig.getVersion();
            }

            @Override
            public String getName() {
                return orig.getName();
            }

            @Override
            public String getKey() {
                return orig.getKey();
            }

            @Override
            public Date getCreationDate() {
                return orig.getCreationDate();
            }

            @Override
            public Date getReleaseDate() {
                return orig.getReleaseDate();
            }

            @Override
            public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
                return orig.getPluginSnapshots();
            }
        }

        ReferenceRuntime refSnapshot = new ReferenceRuntime(snapshot);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        refSnapshot.writeJson(sink);
        checkContentEquals("test-export.json", sink.toString(), true, true);
    }

    /**
     * Reads a reference export kept in tests, to detect potential compatibility changes.
     *
     * @implNote reference file "test-export.json" is initialized thanks to above method, keeping only a few operations
     *           and bundles.
     * @since 11.1
     */
    @Test
    public void canReadLegacy() throws IOException {
        RuntimeSnapshot runtimeSnapshot = RuntimeSnapshot.build();
        String export = getReferenceContent(getReferencePath("test-export.json"));
        try (ByteArrayInputStream source = new ByteArrayInputStream(export.getBytes())) {
            DistributionSnapshot snapshot = runtimeSnapshot.readJson(source);
            checkSnapshot(snapshot, true);
        }
    }

    protected void checkSnapshot(DistributionSnapshot snapshot, boolean legacy) throws IOException {
        assertNotNull(snapshot);
        assertEquals("Nuxeo", snapshot.getName());
        assertEquals("unknown", snapshot.getVersion());
        assertNotNull(snapshot.getCreationDate());
        assertEquals("Nuxeo-unknown", snapshot.getKey());

        BundleInfo bundle = snapshot.getBundle("org.nuxeo.apidoc.repo");
        assertNotNull(bundle);
        assertEquals("nuxeo-apidoc-repo", bundle.getArtifactId());
        assertEquals(BundleInfo.TYPE_NAME, bundle.getArtifactType());

        String version = "11.1-SNAPSHOT";
        if (legacy) {
            assertEquals(version, bundle.getArtifactVersion());
        } else {
            version = bundle.getArtifactVersion();
            assertNotNull(version);
            assertTrue(version.trim().length() > 0);
        }

        assertEquals("org.nuxeo.apidoc.repo", bundle.getBundleId());
        assertEquals("org.nuxeo.ecm.platform", bundle.getGroupId());
        assertEquals("/grp:org.nuxeo.ecm.platform/org.nuxeo.apidoc.repo", bundle.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.repo", bundle.getId());
        assertNull(bundle.getLiveDoc());
        assertEquals("Manifest-Version: 1.0\n" //
                + "Bundle-ManifestVersion: 1\n" //
                + "Bundle-Name: nuxeo api documentation repository\n" //
                + "Bundle-SymbolicName: org.nuxeo.apidoc.repo;singleton:=true\n" //
                + "Bundle-Version: 0.0.1\n" //
                + "Bundle-Vendor: Nuxeo\n" //
                + "Nuxeo-Component: OSGI-INF/schema-contrib.xml,\n" //
                + "  OSGI-INF/doctype-contrib.xml,\n" + "  OSGI-INF/life-cycle-contrib.xml,\n" //
                + "  OSGI-INF/snapshot-service-framework.xml,\n" //
                + "  OSGI-INF/documentation-service-framework.xml,\n" //
                + "  OSGI-INF/adapter-contrib.xml,\n" //
                + "  OSGI-INF/directories-contrib.xml,\n" //
                + "  OSGI-INF/listener-contrib.xml\n"//
                + "", bundle.getManifest());
        assertNull(bundle.getParentLiveDoc());
        assertEquals(Collections.emptyList(), bundle.getRequirements());
        assertEquals(version, bundle.getVersion());

        // check components
        List<ComponentInfo> components = bundle.getComponents();
        assertNotNull(components);
        assertEquals(9, components.size());
        ComponentInfo smcomp = snapshot.getComponent("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");
        assertNotNull(smcomp);
        assertEquals(ComponentInfo.TYPE_NAME, smcomp.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getComponentClass());
        assertNull(smcomp.getDocumentation());
        assertEquals("", smcomp.getDocumentationHtml());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                smcomp.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getId());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", smcomp.getName());
        assertEquals(version, smcomp.getVersion());
        assertFalse(smcomp.isXmlPureComponent());
        checkContentEquals("OSGI-INF/snapshot-service-framework.xml", smcomp.getXmlFileContent());

        // check json back reference
        assertNotNull(smcomp.getBundle());
        assertEquals("org.nuxeo.apidoc.repo", smcomp.getBundle().getId());

        // check services
        assertNotNull(smcomp.getServices());
        assertEquals(1, smcomp.getServices().size());
        ServiceInfo service = smcomp.getServices().get(0);
        assertEquals(ServiceInfo.TYPE_NAME, service.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", service.getComponentId());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent/Services/org.nuxeo.apidoc.snapshot.SnapshotManager",
                service.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManager", service.getId());
        assertEquals(version, service.getVersion());
        assertFalse(service.isOverriden());
        // check json back reference
        assertNotNull(service.getComponent());

        // check extension points
        assertNotNull(smcomp.getExtensionPoints());
        assertEquals(1, smcomp.getExtensionPoints().size());
        ExtensionPointInfo xp = smcomp.getExtensionPoints().get(0);
        assertEquals(ExtensionPointInfo.TYPE_NAME, xp.getArtifactType());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", xp.getComponentId());
        assertNotNull(xp.getDocumentation());
        assertNotNull(xp.getDocumentationHtml());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent/ExtensionPoints/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                xp.getHierarchyPath());
        assertEquals("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins", xp.getId());
        assertEquals("plugins (org.nuxeo.apidoc.snapshot.SnapshotManagerComponent)", xp.getLabel());
        assertEquals("plugins", xp.getName());
        assertEquals(version, xp.getVersion());
        assertNotNull(xp.getDescriptors());
        assertEquals(1, xp.getDescriptors().length);
        assertEquals("org.nuxeo.apidoc.plugin.PluginDescriptor", xp.getDescriptors()[0]);
        // check json back reference
        assertNotNull(xp.getComponent());

        // check extensions
        assertNotNull(smcomp.getExtensions());
        assertEquals(0, smcomp.getExtensions().size());

        // check another component with contributions
        ComponentInfo smcont = snapshot.getComponent("org.nuxeo.apidoc.doctypeContrib");
        assertNotNull(smcont);
        assertNotNull(smcont.getExtensions());
        assertEquals(1, smcont.getExtensions().size());
        ExtensionInfo ext = smcont.getExtensions().get(0);
        assertEquals(ExtensionInfo.TYPE_NAME, ext.getArtifactType());
        assertNotNull(ext.getContributionItems());
        assertEquals(9, ext.getContributionItems().size());
        assertEquals("NXDistribution", ext.getContributionItems().get(0).getId());
        assertEquals("doctype NXDistribution", ext.getContributionItems().get(0).getLabel());
        assertNotNull(ext.getContributionItems().get(0).getXml());
        assertNotNull(ext.getContributionItems().get(0).getRawXml());
        assertEquals("org.nuxeo.ecm.core.schema.TypeService--doctype", ext.getExtensionPoint());
        assertEquals(
                "/grp:org.nuxeo.ecm.platform/org.nuxeo.apidoc.repo/org.nuxeo.apidoc.doctypeContrib/Contributions/org.nuxeo.apidoc.doctypeContrib--doctype",
                ext.getHierarchyPath());
        assertEquals(new ComponentName("service:org.nuxeo.ecm.core.schema.TypeService"), ext.getTargetComponentName());
        assertEquals(version, ext.getVersion());
        // check json back reference
        assertNotNull(ext.getComponent());

        // check operations
        List<OperationInfo> operations = snapshot.getOperations();
        assertNotNull(operations);
        assertEquals(2, operations.size());
        OperationInfo op = operations.get(0);
        assertNotNull(op);
        assertEquals(OperationInfo.TYPE_NAME, op.getArtifactType());
        assertEquals("Services", op.getCategory());
        assertEquals("org.nuxeo.ecm.core.automation.features.operations", op.getContributingComponent());
        assertEquals(
                "Retrieve list of available actions for a given category. Action context is built based on the Operation context "
                        + "(currentDocument will be fetched from Context if not provided as input). If this operation is executed in a chain"
                        + " that initialized the Seam context, it will be used for Action context",
                op.getDescription());
        assertEquals("/op:Actions.GET", op.getHierarchyPath());
        assertEquals("op:Actions.GET", op.getId());
        assertEquals("List available actions", op.getLabel());
        assertEquals("Actions.GET", op.getName());
        assertNotNull(op.getParams());
        assertEquals(2, op.getParams().size());
    }

}
