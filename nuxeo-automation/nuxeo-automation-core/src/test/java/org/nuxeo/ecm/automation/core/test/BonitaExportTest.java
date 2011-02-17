/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.doc.BonitaExporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 5.4.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class BonitaExportTest {

    @Test
    public void generateXMLCode() throws Exception {
        AutomationService service = Framework.getService(AutomationService.class);
        OperationType op = service.getOperation("Document.Create");
        Assert.assertEquals("xml description content",
                BonitaExporter.getXMLDescription(op.getDocumentation()));
    }

    @Test
    public void generateJavaCode() throws Exception {
        AutomationService service = Framework.getService(AutomationService.class);
        OperationType op = service.getOperation("Document.Create");
        Assert.assertEquals("java class content",
                BonitaExporter.getJavaClass(op.getDocumentation()));
    }
}
