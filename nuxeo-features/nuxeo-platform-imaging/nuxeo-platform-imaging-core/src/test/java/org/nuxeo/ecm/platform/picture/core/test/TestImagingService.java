/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author btatar
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-service-framework.xml")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-service-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/picture-schemas-contrib.xml")
public class TestImagingService {

    @Inject
    protected ImagingService imagingService;

    @Test
    public void testConfigurationContrib() throws Exception {
        String conversionFormat = imagingService.getConfigurationValue("conversionFormat", "png");

        assertEquals("jpg", conversionFormat);
        assertNotSame("png", conversionFormat);
    }

    @Test
    public void testUnregisteredConfiguration() throws Exception {
        String testConfiguration = imagingService.getConfigurationValue("testConfiguration");
        assertNull(testConfiguration);

        imagingService.setConfigurationValue("testConfiguration", "testConfigurationValue");
        testConfiguration = imagingService.getConfigurationValue("testConfiguration", "testConfiguration");

        assertEquals("testConfigurationValue", testConfiguration);
        assertNotSame("testConfiguration", testConfiguration);
    }
}
