/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author btatar
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-service-framework.xml",
        "org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-service-contrib.xml" })
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
