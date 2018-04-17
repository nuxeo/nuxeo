/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.platform.forms.layout.core")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export")
@Deploy("org.nuxeo.ecm.platform.forms.layout.client:OSGI-INF/layouts-framework.xml")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export.tests:OSGI-INF/layouts-test-contrib.xml")
public class TestLayoutExport {

    @Inject
    public LayoutStore service;

    @Inject
    public MarshallerRegistry marshallerRegistry;

    protected void checkEquals(InputStream expected, InputStream actual) throws Exception {
        String expectedString = IOUtils.toString(expected, UTF_8).replaceAll("\r?\n", "");
        String actualString = IOUtils.toString(actual, UTF_8).replaceAll("\r?\n", "");
        JSONAssert.assertEquals(expectedString, actualString, true);
    }

    @Test
    public void testLayoutTypeExport() throws Exception {
        LayoutTypeDefinition lTypeDef = service.getLayoutTypeDefinition(WebLayoutManager.JSF_CATEGORY, "listing");
        assertNotNull(lTypeDef);

        File file = Framework.createTempFile("layouttype-export", ".json");
        writeJsonToFile(lTypeDef, file);

        try (InputStream written = new FileInputStream(file);
                InputStream expected = new FileInputStream(
                        FileUtils.getResourcePathFromContext("layouttype-export.json"))) {
            checkEquals(expected, written);
        }
    }

    @Test
    public void testWidgetTypeExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition(WebLayoutManager.JSF_CATEGORY, "test");
        assertNotNull(wTypeDef);

        File file = Framework.createTempFile("widgettype-export", ".json");
        writeJsonToFile(wTypeDef, file);

        try (InputStream written = new FileInputStream(file);
                InputStream expected = new FileInputStream(
                        FileUtils.getResourcePathFromContext("widgettype-export.json"))) {
            checkEquals(expected, written);
        }
    }

    @Test
    public void testWidgetTypesExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition(WebLayoutManager.JSF_CATEGORY, "test");
        assertNotNull(wTypeDef);

        File file = Framework.createTempFile("widgettypes-export", ".json");
        WidgetTypeDefinitions wTypeDefs = new WidgetTypeDefinitions();
        wTypeDefs.add(wTypeDef);
        writeJsonToFile(wTypeDefs, file);

        try (InputStream written = new FileInputStream(file);
                InputStream expected = new FileInputStream(
                        FileUtils.getResourcePathFromContext("widgettypes-export.json"))) {
            checkEquals(expected, written);
        }
    }

    @Test
    public void testLayoutExport() throws Exception {
        LayoutDefinition lDef = service.getLayoutDefinition(WebLayoutManager.JSF_CATEGORY, "layoutColumnsTest");
        assertNotNull(lDef);

        File file = Framework.createTempFile("layout-export", ".json");
        writeJsonToFile(CtxBuilder.param(LayoutExportConstants.CATEGORY_PARAMETER, WebLayoutManager.JSF_CATEGORY).get(),
                lDef, file);

        try (InputStream written = new FileInputStream(file);
                InputStream expected = new FileInputStream(
                        FileUtils.getResourcePathFromContext("layout-export.json"))) {
            checkEquals(expected, written);
        }
    }

    protected <T> void writeJsonToFile(T entity, File file) throws IOException {
        writeJsonToFile(CtxBuilder.get(), entity, file);
    }

    protected <T> void writeJsonToFile(RenderingContext ctx, T entity, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) entity.getClass();
            Writer<T> writer = marshallerRegistry.getWriter(ctx, type, APPLICATION_JSON_TYPE);
            writer.write(entity, type, type, APPLICATION_JSON_TYPE, out);
        }
    }

}
