/**
 *
 */
package org.nuxeo.ecm.platform.workflow.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemInstanceImpl;
import org.nuxeo.ecm.platform.workflow.document.security.policy.OrderedReviewWorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.testing.FakeOrderedReviewWorkflowDocumentSecurityPolicy;

/**
 * Test the parallel security policy implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
// FIXME: this doesn't test ParallelSecurityPolicy but
// OrderedReviewWorkflowDocumentSecurityPolicy
public class TestParallelSecurityPolicy extends TestCase {

    protected static final Random random = new Random(new Date().getTime());

    OrderedReviewWorkflowDocumentSecurityPolicy policy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private String generateIdentifier() {
        return String.valueOf(random.nextInt());
    }

    public void testFixtures() throws WMWorkflowException {

        // Build tasks list.
        Collection<WMWorkItemInstance> tasks = new ArrayList<WMWorkItemInstance>();

        tasks.add(new WMWorkItemInstanceImpl(generateIdentifier(),
                new WMParticipantImpl("a"), 0, false, false, false));

        policy = new FakeOrderedReviewWorkflowDocumentSecurityPolicy(tasks);
        // :XXX
        //policy.getRules(null, null);
    }

    @Override
    protected void tearDown() throws Exception {
        policy = null;
        super.tearDown();
    }

}
