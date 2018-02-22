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

import org.junit.Assert;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.pipe.local.LocalEventBundlePipe;
import org.nuxeo.ecm.core.event.test.TestEventServiceComponent;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Run the existing EventListeners tests using the LocalPipe implementation.
 *
 * @since 8.4
 */
@Deploy("org.nuxeo.ecm.core.event.test:test-LocalPipes.xml")
public class TestEventListenerViaLocalPipe extends TestEventServiceComponent {

    @Override
    protected EventServiceImpl getService() {
        EventServiceImpl service = super.getService();
        Assert.assertEquals(service.getEventBundleDispatcher().getClass(),
                TestableSimpleEventBundlePipeDispatcher.class);
        TestableSimpleEventBundlePipeDispatcher dispatcher = (TestableSimpleEventBundlePipeDispatcher) service.getEventBundleDispatcher();
        Assert.assertEquals(dispatcher.getPipes().get(0).getClass(), LocalEventBundlePipe.class);
        return service;
    }

}
