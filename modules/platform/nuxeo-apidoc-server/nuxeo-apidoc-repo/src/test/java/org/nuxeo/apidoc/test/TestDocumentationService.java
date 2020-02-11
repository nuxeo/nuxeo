/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestDocumentationService {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentationService ds;

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void testService() throws Exception {
        DistributionSnapshot runtimeSnapshot = snapshotManager.getRuntimeSnapshot();

        BundleGroup bg = runtimeSnapshot.getBundleGroup("org.nuxeo.ecm.core");
        assertNotNull(bg);
        doTestDocumentationOnArtifact(bg);

        BundleInfo bi = runtimeSnapshot.getBundle("org.nuxeo.ecm.core");
        assertNotNull(bi);
        doTestDocumentationOnArtifact(bi);

        ComponentInfo ci = runtimeSnapshot.getComponent("org.nuxeo.ecm.core.CoreExtensions");
        assertNotNull(ci);
        doTestDocumentationOnArtifact(ci);

        ExtensionPointInfo epi = runtimeSnapshot.getExtensionPoint(
                "org.nuxeo.ecm.core.lifecycle.LifeCycleService--types");
        assertNotNull(epi);
        doTestDocumentationOnArtifact(epi);

    }

    protected void doTestDocumentationOnArtifact(NuxeoArtifact artifact) throws Exception {
        List<String> applicableVersions = new ArrayList<>();
        applicableVersions.add(artifact.getVersion());

        // create and verify
        DocumentationItem item = ds.createDocumentationItem(session, artifact, "testTitle", "testContent", "",
                applicableVersions, true, "");
        assertNotNull(item);
        List<DocumentationItem> variants = ds.findDocumentationItemVariants(session, item);
        assertEquals(1, variants.size());
        assertEquals("testTitle", variants.get(0).getTitle());
        assertEquals("testContent", variants.get(0).getContent());

        List<DocumentationItem> foundItems = ds.findDocumentItems(session, artifact);
        assertEquals(1, foundItems.size());
        assertEquals("testTitle", foundItems.get(0).getTitle());
        assertEquals("testContent", foundItems.get(0).getContent());

        // update without version
        FakeDocumentationItem updatedItem = new FakeDocumentationItem(item);
        updatedItem.content = "newContent";
        item = ds.updateDocumentationItem(session, updatedItem);
        assertNotNull(item);
        variants = ds.findDocumentationItemVariants(session, item);
        assertEquals(1, variants.size());
        assertEquals("testTitle", variants.get(0).getTitle());
        assertEquals("newContent", variants.get(0).getContent());

        foundItems = ds.findDocumentItems(session, artifact);
        assertEquals(1, foundItems.size());
        assertEquals("testTitle", foundItems.get(0).getTitle());
        assertEquals("newContent", foundItems.get(0).getContent());

        // update with version addition
        updatedItem.content = "newContent2";
        updatedItem.applicableVersion.add("v2");
        item = ds.updateDocumentationItem(session, updatedItem);
        assertNotNull(item);
        variants = ds.findDocumentationItemVariants(session, item);
        assertEquals(1, variants.size());
        assertEquals("testTitle", variants.get(0).getTitle());
        assertEquals("newContent2", variants.get(0).getContent());

        foundItems = ds.findDocumentItems(session, artifact);
        assertEquals(1, foundItems.size());
        assertEquals("testTitle", foundItems.get(0).getTitle());
        assertEquals("newContent2", foundItems.get(0).getContent());

        // update with new version
        updatedItem.content = "newContent3";
        updatedItem.applicableVersion.clear();
        updatedItem.applicableVersion.add("v3");
        item = ds.updateDocumentationItem(session, updatedItem);
        assertNotNull(item);
        variants = ds.findDocumentationItemVariants(session, item);
        assertEquals(2, variants.size());
        assertEquals("testTitle", variants.get(0).getTitle());
        assertEquals("newContent3", variants.get(0).getContent());
        assertEquals("testTitle", variants.get(1).getTitle());
        assertEquals("newContent2", variants.get(1).getContent());

        foundItems = ds.findDocumentItems(session, artifact);
        assertEquals(1, foundItems.size());
        assertEquals("testTitle", foundItems.get(0).getTitle());
        assertEquals("newContent2", foundItems.get(0).getContent());

        FakeNuxeoArtifact testArtifact = new FakeNuxeoArtifact(artifact);
        testArtifact.version = "v3";
        foundItems = ds.findDocumentItems(session, testArtifact);
        assertEquals(1, foundItems.size());
        assertEquals("testTitle", foundItems.get(0).getTitle());
        assertEquals("newContent3", foundItems.get(0).getContent());

    }

}
