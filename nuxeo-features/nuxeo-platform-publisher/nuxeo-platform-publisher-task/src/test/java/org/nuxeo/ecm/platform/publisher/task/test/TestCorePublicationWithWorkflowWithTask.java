package org.nuxeo.ecm.platform.publisher.task.test;

import org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory;

public class TestCorePublicationWithWorkflowWithTask extends
        TestCorePublicationWithWorkflow {

    {
        factoryParams.put(CoreProxyWithWorkflowFactory.LOOKUP_STATE_PARAM_KEY,
                CoreProxyWithWorkflowFactory.LOOKUP_STATE_PARAM_BYTASK);
    }
}
