/**
 *
 */
package org.nuxeo.ecm.platform.workflow.document;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.document.security.policy.OrderedReviewWorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.testing.FakeOrderedReviewWorkflowDocumentSecurityPolicy;

/**
 * Test the ordered security policy implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestOrderedSecurityPolicy extends TestCase {

    OrderedReviewWorkflowDocumentSecurityPolicy policy;

    @Override
    protected void tearDown() throws Exception {
        policy = null;
        super.tearDown();
    }

    public void testFixtures() {
        Collection<WMWorkItemInstance> tasks = new ArrayList<WMWorkItemInstance>();
        policy = new FakeOrderedReviewWorkflowDocumentSecurityPolicy(tasks);
    }

}
