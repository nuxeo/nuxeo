package org.nuxeo.snapshot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.snapshot.operation.CreateSnapshot;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(type = BackendType.H2, init = PublishRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.snapshot" })
public class TestSnapshotOperations extends AbstractTestSnapshot {

    @Test
    public void testSnapshotOperation() throws Exception {
        buildTree();
        folderB1.addFacet(Snapshotable.FACET);
        session.save();

        AutomationService as = Framework.getLocalService(AutomationService.class);
        assertNotNull(as);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(folderB1);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("versioning option", VersioningOption.MAJOR.name());
        DocumentModel version = (DocumentModel) as.run(ctx, CreateSnapshot.ID,
                params);

        assertTrue(version.isVersion());
        assertEquals("1.0", version.getVersionLabel());
    }
}
