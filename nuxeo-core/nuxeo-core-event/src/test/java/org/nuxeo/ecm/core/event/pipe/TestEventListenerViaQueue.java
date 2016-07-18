/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import java.net.URL;

import org.junit.Before;
import org.nuxeo.ecm.core.event.impl.PostCommitEventExecutor;
import org.nuxeo.ecm.core.event.test.DummyPostCommitEventListener;
import org.nuxeo.ecm.core.event.test.EventListenerTest;
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
        URL url = EventListenerTest.class.getClassLoader().getResource("test-LocalPipes.xml");
        deployTestContrib("org.nuxeo.ecm.core.event.test", url);

        fireFrameworkStarted();
        // 2 quartz threads launched by the event contribs above
        Thread.sleep(100);
        initialThreadCount = Thread.activeCount();
        DummyPostCommitEventListener.handledCountReset();
        DummyPostCommitEventListener.eventCountReset();
    }
}
