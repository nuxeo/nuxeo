package org.nuxeo.ecm.admin.operation;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.admin.permissions.AbstractPermissionsPurge;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@LocalDeploy({ "org.nuxeo.admin.center:OSGI-INF/operation-contrib.xml" })
public class TestPermissionsPurgeOperation extends AbstractPermissionsPurge {

    @Inject
    protected AutomationService automationService;

    @Override
    public void scheduleWork(DocumentModel doc) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        automationService.run(ctx, PermissionsPurge.ID, Collections.emptyMap());
    }
}
