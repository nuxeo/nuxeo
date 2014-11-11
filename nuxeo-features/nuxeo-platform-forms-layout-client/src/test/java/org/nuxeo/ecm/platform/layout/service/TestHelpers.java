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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TestHelpers.java 26808 2007-11-05 12:00:39Z atchertchian $
 */

package org.nuxeo.ecm.platform.layout.service;

import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.FieldDescriptor;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class TestHelpers extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-schemas.xml");
    }

    public void testValueExpressionHelper() {
        FieldDefinition fieldDef = new FieldDescriptor("dublincore", "title");
        String expression = ValueExpressionHelper.createExpressionString(
                "document", fieldDef);
        assertEquals("#{document.dublincore.title}", expression);
        fieldDef = new FieldDescriptor(null, "dc:title");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document.dublincore.title}", expression);
        fieldDef = new FieldDescriptor(null, "dc:contributors/0/name");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document.dublincore.contributors[0].name}", expression);
    }

}
