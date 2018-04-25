/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.io.plugins.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.export.LayoutExportConstants;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.forms.layout.core")
@Deploy("org.nuxeo.ecm.platform.forms.layout.client")
@Deploy("org.nuxeo.ecm.platform.forms.layout.io.plugins")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export")
@Deploy("org.nuxeo.ecm.platform.forms.layout.io.plugins:layouts-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.forms.layout.io.plugins:test-directories-contrib.xml")
public class TestLayoutExport {

    protected static final String TEST_CATEGORY = "standalone";

    @Inject
    protected LayoutStore service;

    @Inject
    protected WebLayoutManager jsfService;

    @Inject
    protected MarshallerRegistry marshallerRegistry;

    @Test
    public void testLayoutDefinitionExport() throws Exception {
        LayoutDefinition layoutDef = service.getLayoutDefinition(WebLayoutManager.JSF_CATEGORY, "dublincore");
        assertNotNull(layoutDef);

        check(layoutDef, "en");
        check(layoutDef, "fr");
        check(layoutDef, null);
    }

    protected void check(LayoutDefinition layoutDef, String lang) throws Exception {
        LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
        List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(TEST_CATEGORY);
        for (LayoutDefinitionConverter conv : layoutConverters) {
            layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
        }
        List<WidgetDefinitionConverter> widgetConverters = service.getWidgetConverters(TEST_CATEGORY);

        String langFilePath = lang;
        if (langFilePath == null) {
            langFilePath = "nolang";
        }
        File file = Framework.createTempFile("layout-export-" + langFilePath, ".json");
        RenderingContext renderingCtx = CtxBuilder.param(
                LayoutExportConstants.CATEGORY_PARAMETER, WebLayoutManager.JSF_CATEGORY)
                                                  .param(LayoutExportConstants.LAYOUT_CONTEXT_PARAMETER, ctx)
                                                  .paramList(LayoutExportConstants.WIDGET_CONVERTERS_PARAMETER,
                                                          widgetConverters)
                                                  .get();
        writeJsonToFile(renderingCtx, layoutDef, file);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("layout-export-" + langFilePath + ".json"));

        String expectedString = IOUtils.toString(expected, UTF_8);
        String writtenString = IOUtils.toString(written, UTF_8);
        // order of select options may depend on directory database => do not
        // check order of element by using the NON_EXTENSIBLE mode
        JSONAssert.assertEquals(expectedString, writtenString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testLayoutInstanceExport() throws Exception {
        check("dublincore", "en");
        check("dublincore", "fr");
        check("dublincore", null);
    }

    protected void check(String layoutName, String lang) throws Exception {
        LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
        LayoutDefinition layoutDef = service.getLayoutDefinition(WebLayoutManager.JSF_CATEGORY, layoutName);
        Layout layout = jsfService.getLayout(null, ctx, TEST_CATEGORY, layoutDef, BuiltinModes.VIEW, "currentDocument",
                null, false);
        String langFilePath = lang;
        if (langFilePath == null) {
            langFilePath = "nolang";
        }
        File file = Framework.createTempFile("layout-instance-export-" + langFilePath, ".json");
        writeJsonToFile(CtxBuilder.get(), layout, file);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("layout-instance-export-" + langFilePath + ".json"));

        String expectedString = IOUtils.toString(expected, UTF_8);
        String writtenString = IOUtils.toString(written, UTF_8);
        // order of select options may depend on directory database => do not
        // check order of element by using the NON_EXTENSIBLE mode
        JSONAssert.assertEquals(expectedString, writtenString, JSONCompareMode.NON_EXTENSIBLE);
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
