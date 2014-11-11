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
package org.nuxeo.ecm.platform.forms.layout.io.plugins.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.forms.layout.core",
        "org.nuxeo.ecm.platform.forms.layout.client",
        "org.nuxeo.ecm.platform.forms.layout.io.plugins" })
@LocalDeploy({
        "org.nuxeo.ecm.platform.forms.layout.io.plugins:layouts-test-contrib.xml",
        "org.nuxeo.ecm.platform.forms.layout.io.plugins:test-directories-contrib.xml" })
public class TestLayoutExport {

    protected static final String TEST_CATEGORY = "standalone";

    @Inject
    protected LayoutStore service;

    @Test
    public void testLayoutDefinitionExport() throws Exception {
        LayoutDefinition layoutDef = service.getLayoutDefinition(
                WebLayoutManager.JSF_CATEGORY, "dublincore");
        assertNotNull(layoutDef);

        check(layoutDef, "en");
        check(layoutDef, "fr");
        check(layoutDef, null);
    }

    protected void check(LayoutDefinition layoutDef, String lang)
            throws Exception {
        LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
        List<LayoutDefinitionConverter> layoutConverters = service
            .getLayoutConverters(TEST_CATEGORY);
        for (LayoutDefinitionConverter conv : layoutConverters) {
            layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
        }
        List<WidgetDefinitionConverter> widgetConverters = service
            .getWidgetConverters(TEST_CATEGORY);

        String langFilePath = lang;
        if (langFilePath == null) {
            langFilePath = "nolang";
        }
        File file = File.createTempFile("layout-export-" + langFilePath,
                ".json");
        FileOutputStream out = new FileOutputStream(file);
        JSONLayoutExporter.export(WebLayoutManager.JSF_CATEGORY, layoutDef,
                ctx, widgetConverters, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("layout-export-"
                        + langFilePath + ".json"));

        String expectedString = FileUtils.read(expected);
        String writtenString = FileUtils.read(written);
        // order of select options may depend on directory database => do not
        // check order of element by using the NON_EXTENSIBLE mode
        JSONAssert.assertEquals(expectedString, writtenString,
                JSONCompareMode.NON_EXTENSIBLE);
    }

}
