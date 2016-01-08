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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import net.sf.json.JSONObject;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertNotNull;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.forms.layout.core", "org.nuxeo.ecm.platform.forms.layout.client",
        "org.nuxeo.ecm.platform.forms.layout.io.plugins" })
@LocalDeploy({ "org.nuxeo.ecm.platform.forms.layout.io.plugins:layouts-test-contrib.xml",
        "org.nuxeo.ecm.platform.forms.layout.io.plugins:test-directories-contrib.xml" })
public class TestLayoutExport {

    protected static final String TEST_CATEGORY = "standalone";

    @Inject
    protected LayoutStore service;

    @Inject
    protected WebLayoutManager jsfService;

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
        FileOutputStream out = new FileOutputStream(file);
        JSONLayoutExporter.export(WebLayoutManager.JSF_CATEGORY, layoutDef, ctx, widgetConverters, out);
        out.close();

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(FileUtils.getResourcePathFromContext("layout-export-" + langFilePath
                + ".json"));

        String expectedString = IOUtils.toString(expected, Charsets.UTF_8);
        String writtenString = IOUtils.toString(written, Charsets.UTF_8);
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
        FileOutputStream out = new FileOutputStream(file);
        JSONObject res = JSONLayoutExporter.exportToJson(layout);
        out.write(res.toString(2).getBytes(JSONLayoutExporter.ENCODED_VALUES_ENCODING));
        out.close();

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(FileUtils.getResourcePathFromContext("layout-instance-export-"
                + langFilePath + ".json"));

        String expectedString = IOUtils.toString(expected, Charsets.UTF_8);
        String writtenString = IOUtils.toString(written, Charsets.UTF_8);
        // order of select options may depend on directory database => do not
        // check order of element by using the NON_EXTENSIBLE mode
        JSONAssert.assertEquals(expectedString, writtenString, JSONCompareMode.NON_EXTENSIBLE);
    }

}
