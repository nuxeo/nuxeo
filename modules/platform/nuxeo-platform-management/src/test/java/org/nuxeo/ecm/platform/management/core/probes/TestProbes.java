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
 *     matic
 */
package org.nuxeo.ecm.platform.management.core.probes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.management.statuses.ProbeScheduler;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.core.management")
@Deploy("org.nuxeo.ecm.platform.management")
public class TestProbes {

    @Inject
    protected ProbeScheduler scheduler;

    @Inject
    protected ResourcePublisher publisher;

    @Inject
    protected ProbeManager runner;

    @Test
    public void testScheduling() throws MalformedObjectNameException {
        assertFalse(scheduler.isEnabled());

        scheduler.enable();
        assertTrue(scheduler.isEnabled());

        scheduler.disable();
        assertFalse(scheduler.isEnabled());

        assertTrue(publisher.getResourcesName().contains(new ObjectName("org.nuxeo:name=probeScheduler,type=service")));
    }

    @Test
    public void testPopulateRepository() throws Exception {
        ProbeInfo info = runner.getProbeInfo("populateRepository");
        assertNotNull(info);
        info = runner.runProbe(info);
        assertFalse(info.isInError());
        String result = info.getStatus().getAsString();
        System.out.print("populateRepository Probe result : " + result);
    }

    @Test
    public void testQueryRepository() throws Exception {
        ProbeInfo info = runner.getProbeInfo("queryRepository");
        assertNotNull(info);
        info = runner.runProbe(info);
        assertFalse(info.isInError());
        System.out.print(info.getStatus().getAsString());
    }

}
