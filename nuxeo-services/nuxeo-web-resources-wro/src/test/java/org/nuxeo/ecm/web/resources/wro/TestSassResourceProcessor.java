/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.wro;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroModelFactory;
import org.nuxeo.ecm.web.resources.wro.processor.SassCssFlavorProcessor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.model.factory.DefaultWroModelFactoryDecorator;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.util.WroTestUtils;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.theme.styling")
@Deploy("org.nuxeo.web.resources.core")
@Deploy("org.nuxeo.web.resources.wro")
@Deploy("org.nuxeo.web.resources.wro:webresources-test-config.xml")
@Deploy("org.nuxeo.web.resources.wro:theme-styling-test-config.xml")
public class TestSassResourceProcessor {

    @Inject
    protected WebResourceManager service;

    @Mock
    private Reader mockReader;

    @Mock
    private Writer mockWriter;

    @Mock
    private HttpServletRequest mockRequest;

    private ResourcePreProcessor victim;

    @BeforeClass
    public static void onBeforeClass() {
        assertEquals(0, Context.countActive());
    }

    @AfterClass
    public static void onAfterClass() {
        assertEquals(0, Context.countActive());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context.set(Context.webContext(mockRequest, mock(HttpServletResponse.class), mock(FilterConfig.class)));
        WroModelFactory factory = DefaultWroModelFactoryDecorator.decorate(new NuxeoWroModelFactory(),
                Collections.emptyList());
        WroTestUtils.init(factory);
        victim = new SassCssFlavorProcessor();
        WroTestUtils.initProcessor(victim);
    }

    @After
    public void tearDown() {
        Context.unset();
    }

    @Test
    public void shouldSupportCorrectResourceTypes() {
        WroTestUtils.assertProcessorSupportResourceTypes(new SassCssFlavorProcessor(), ResourceType.CSS);
    }

    @Test
    public void testSassResourceProcessor() throws Exception {
        when(mockRequest.getQueryString()).thenReturn("uri?flavor=default");
        final Reader reader = new InputStreamReader(getTestFile("themes/sass/app.scss"));
        final StringWriter writer = new StringWriter();
        victim.process(Resource.create(org.nuxeo.ecm.web.resources.api.Resource.PREFIX + "sass_app.scss",
                ResourceType.CSS), reader, writer);
        ByteArrayInputStream bais = new ByteArrayInputStream(writer.toString().getBytes());
        WroTestUtils.compare(getTestFile("wro/sass_css_rendering.txt"), bais);
    }

    protected static InputStream getTestFile(String filePath) throws Exception {
        return new FileInputStream(FileUtils.getResourcePathFromContext(filePath));
    }

}
