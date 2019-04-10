package org.nuxeo.snapshot;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.snapshot.CreateLeafListener.DO_NOT_CHANGE_CHILD_FLAG;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(type = BackendType.H2, init = PublishRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.snapshot" })
@LocalDeploy({ "org.nuxeo.snapshot:snapshot-listener-contrib.xml" })
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
