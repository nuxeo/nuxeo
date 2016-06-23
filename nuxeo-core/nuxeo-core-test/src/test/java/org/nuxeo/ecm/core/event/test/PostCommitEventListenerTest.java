/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * PostCommitEventListenerTest test ScriptingPostCommitEventListener
 *
 * @author <a href="mailto:jt@nuxeo.com">Julien THIMONIER</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class PostCommitEventListenerTest {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreIsolated.class)
    public void testScripts() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.test.tests", "test-PostCommitListeners.xml");

        assertEquals(0, SCRIPT_CNT);

        EventContextImpl customContext = new EventContextImpl(null, null);
        customContext.setProperty("cle", "valeur");
        customContext.setProperty("cle2", "valeur2");

        EventService service = Framework.getService(EventService.class);
        service.fireEvent("test", customContext);
        assertEquals(0, SCRIPT_CNT);

        service.fireEvent("test1", customContext);
        assertEquals(0, SCRIPT_CNT);

        // this one is filtered out
        service.fireEvent("some-event", customContext);
        assertEquals(0, SCRIPT_CNT);

        session.save();

        nextTransaction();

        service.waitForAsyncCompletion();

        assertEquals(2, SCRIPT_CNT);

        harness.undeployContrib("org.nuxeo.ecm.core.test.tests", "test-PostCommitListeners.xml");
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-ShallowFilteringPostCommitListeners.xml")
    public void testShallowFiltering() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.test.tests", "test-ShallowFilteringPostCommitListeners.xml");

        DocumentModel doc = session.createDocumentModel("/", "empty", "Document");
        doc = session.createDocument(doc);
        ShallowFilterPostCommitEventListener.handledCount = 0;
        session.save();

        nextTransaction();

        assertEquals(1, ShallowFilterPostCommitEventListener.handledCount);

        harness.undeployContrib("org.nuxeo.ecm.core.test.tests", "test-ShallowFilteringPostCommitListeners.xml");
    }

}
