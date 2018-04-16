/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.server.test.UploadFileSupport.MockInputStream;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * @author matic
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@ServletContainer(port = 18080)
@Ignore("NXP-18232")
public class UploadBigFileTest {

    @Inject
    Session session;

    @Test
    public void withMaxMemory() throws Exception {
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        MemoryUsage usage = mbean.getHeapMemoryUsage();
        MockInputStream source = UploadFileSupport.newMockInput(usage.getMax(), false);
        FileInputStream result = new UploadFileSupport(session, "/").testUploadFile(source);
        assertEquals(source.consumed, result.getChannel().size());
    }

}
