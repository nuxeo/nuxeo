/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.junit.Before;
import org.nuxeo.ecm.core.event.impl.PostCommitEventExecutor;
import org.nuxeo.ecm.core.event.test.DummyPostCommitEventListener;
import org.nuxeo.ecm.core.event.test.TestEventServiceComponent;
import org.nuxeo.runtime.api.Framework;

/**
 * Run the existing EventListeners tests using the Queue implementation.
 *
 * @since 8.4
 */
public class TestEventListenerViaQueue extends TestEventServiceComponent {

    @Override
    @Before
    public void setUp() throws Exception {
        System.setProperty("org.nuxeo.runtime.testing", "true");
        // super.setUp();
        wipeRuntime();
        initUrls();
        if (urls == null) {
            throw new UnsupportedOperationException("no bundles available");
        }
        initOsgiRuntime();

        Framework.getProperties().setProperty(PostCommitEventExecutor.TIMEOUT_MS_PROP, "300"); // 0.3s
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test", "test-LocalQueues.xml");

        fireFrameworkStarted();
        // 2 quartz threads launched by the event contribs above
        Thread.sleep(100);
        initialThreadCount = Thread.activeCount();
        DummyPostCommitEventListener.handledCountReset();
        DummyPostCommitEventListener.eventCountReset();
    }
}
