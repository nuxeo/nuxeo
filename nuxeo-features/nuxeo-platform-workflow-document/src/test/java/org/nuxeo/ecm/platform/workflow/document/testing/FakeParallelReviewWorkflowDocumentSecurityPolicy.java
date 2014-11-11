/**
 *
 */
package org.nuxeo.ecm.platform.workflow.document.testing;

import java.security.Principal;
import java.util.Collection;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.security.policy.OrderedReviewWorkflowDocumentSecurityPolicy;

/**
 * Fake policy with tasks as class member so that we can test out them in a unit
 * way.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class FakeParallelReviewWorkflowDocumentSecurityPolicy extends
        OrderedReviewWorkflowDocumentSecurityPolicy {

    private static final long serialVersionUID = -1364441868991931006L;

    protected final Collection<WMWorkItemInstance> tasks;

    public FakeParallelReviewWorkflowDocumentSecurityPolicy(
            Collection<WMWorkItemInstance> tasks) {
        this.tasks = tasks;
    }

    @Override
    protected Collection<WMWorkItemInstance> getTasksFor(String pid, Principal principal)
            throws WMWorkflowException {
        return tasks;
    }

}
