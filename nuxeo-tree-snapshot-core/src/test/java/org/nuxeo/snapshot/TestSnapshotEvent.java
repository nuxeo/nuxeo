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
 *     Arnaud Kervern
 */
package org.nuxeo.snapshot;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.snapshot.CreateLeafListener.DO_NOT_CHANGE_CHILD_FLAG;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = PublishRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.snapshot" })
@Deploy({ "org.nuxeo.snapshot:snapshot-listener-contrib.xml" })
public class TestSnapshotEvent extends AbstractTestSnapshot {

    @Test
    public void testEvent() throws Exception {
        buildTree();

        // Change metadata that will be changed aff
        docB12.setPropertyValue("dc:description", "CHANGE ME XXX");
        session.saveDocument(docB12);

        folderB13.setPropertyValue("dc:description", DO_NOT_CHANGE_CHILD_FLAG);
        session.saveDocument(folderB13);

        docB131.setPropertyValue("dc:description", "CHANGE ME XXX");
        session.saveDocument(docB131);
        session.save();

        folderB1.addFacet(Snapshot.FACET);
        session.save();

        Snapshot adapter = folderB1.getAdapter(Snapshot.class);
        Snapshot snapshot = adapter.createSnapshot(MAJOR);

        session.save();
        dumpDBContent();

        DocumentModel newDoc12 = session.getDocument(docB12.getRef());
        assertEquals("XOXO", newDoc12.getPropertyValue("dc:description"));
        DocumentModel newVersDoc12 = session.getLastDocumentVersion(newDoc12.getRef());
        assertEquals("XOXO", newVersDoc12.getPropertyValue("dc:description"));

        DocumentModel newDocB131 = session.getDocument(docB131.getRef());
        assertEquals("CHANGE ME XXX", newDocB131.getPropertyValue("dc:description"));
        DocumentModel newVersDocB131 = session.getLastDocumentVersion(newDocB131.getRef());
        assertEquals("CHANGE ME XXX", newVersDocB131.getPropertyValue("dc:description"));

    }
}
