/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;

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

import com.google.inject.Inject;

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

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;

    @Test
    @ConditionalIgnoreRule.Ignore(condition=ConditionalIgnoreRule.IgnoreIsolated.class)
    public void testScripts() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.test.tests",
                "test-PostCommitListeners.xml");

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
        Framework.getService(EventService.class).waitForAsyncCompletion();
        assertEquals(2, SCRIPT_CNT);

        harness.undeployContrib("org.nuxeo.ecm.core.test.tests",
                "test-PostCommitListeners.xml");
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-ShallowFilteringPostCommitListeners.xml")
    public void testShallowFiltering() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.test.tests",
                "test-ShallowFilteringPostCommitListeners.xml");

        DocumentModel doc = session.createDocumentModel("/", "empty", "Document");
        doc = session.createDocument(doc);
        ShallowFilterPostCommitEventListener.handledCount = 0;
        session.save();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        assertEquals(1, ShallowFilterPostCommitEventListener.handledCount);

        harness.undeployContrib("org.nuxeo.ecm.core.test.tests",
                "test-ShallowFilteringPostCommitListeners.xml");
    }

}
