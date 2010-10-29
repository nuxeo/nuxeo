/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.layout.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.export.io.WidgetTypeDefinitionJsonExporter;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class TestLayoutExport extends NXRuntimeTestCase {

    private WebLayoutManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client",
                "OSGI-INF/layouts-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.export.tests",
                "layouts-test-contrib.xml");
        service = Framework.getService(WebLayoutManager.class);
        assertNotNull(service);
    }

    public void testWidgetTypeExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
        assertNotNull(wTypeDef);

        File file = File.createTempFile("widgettype-export", ".json");
        FileOutputStream out = new FileOutputStream(file);
        WidgetTypeDefinitionJsonExporter.export(wTypeDef, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("widgettype-export.json"));

        String expectedString = FileUtils.read(expected).replaceAll("\r?\n", "");
        String writtenString = FileUtils.read(written).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

    public void testWidgetTypesExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
        assertNotNull(wTypeDef);

        File file = File.createTempFile("widgettypes-export", ".json");
        FileOutputStream out = new FileOutputStream(file);
        List<WidgetTypeDefinition> wTypeDefs = new ArrayList<WidgetTypeDefinition>();
        wTypeDefs.add(wTypeDef);
        WidgetTypeDefinitionJsonExporter.export(wTypeDefs, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("widgettypes-export.json"));

        String expectedString = FileUtils.read(expected).replaceAll("\r?\n", "");
        String writtenString = FileUtils.read(written).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

}
