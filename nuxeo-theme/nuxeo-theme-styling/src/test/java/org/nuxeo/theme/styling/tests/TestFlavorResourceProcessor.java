/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.tests;

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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.theme.styling.wro.FlavorResourceProcessor;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.model.factory.DefaultWroModelFactoryDecorator;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.util.WroTestUtils;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy({ "org.nuxeo.theme.core", "org.nuxeo.theme.fragments",
        "org.nuxeo.theme.styling", "org.nuxeo.web.resources.core" })
@LocalDeploy({ "org.nuxeo.theme.styling:webresources-test-config.xml",
        "org.nuxeo.theme.styling:theme-styling-test-config.xml" })
public class TestFlavorResourceProcessor {

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
        victim = new FlavorResourceProcessor();
        WroTestUtils.initProcessor(victim);
    }

    @After
    public void tearDown() {
        Context.unset();
    }

    @Test
    public void shouldSupportCorrectResourceTypes() {
        WroTestUtils.assertProcessorSupportResourceTypes(new FlavorResourceProcessor(), ResourceType.CSS);
    }

    @Test
    public void testFlavorResourceProcessor() throws Exception {
        checkFlavorFor("wro/css_default_rendering.txt", "default");
        checkFlavorFor("wro/css_dark_rendering.txt", "dark");
        checkFlavorFor("wro/css_sub_dark_rendering.txt", "subDark");
        checkFlavorFor("wro/css_no_flavor_rendering.txt", "foo");
    }

    protected void checkFlavorFor(String filePath, String flavor) throws Exception {
        when(mockRequest.getQueryString()).thenReturn("uri?flavor=" + flavor);
        final Reader reader = new InputStreamReader(getTestFile("themes/css/nuxeo_dm_default.css"));
        final StringWriter writer = new StringWriter();
        victim.process(Resource.create(org.nuxeo.ecm.web.resources.api.Resource.PREFIX + "nuxeo_dm_default.css", ResourceType.CSS), reader,
                writer);
        WroTestUtils.compare(getTestFile(filePath), new ByteArrayInputStream(writer.toString().getBytes()));
    }

    protected static InputStream getTestFile(String filePath) throws Exception {
        return new FileInputStream(FileUtils.getResourcePathFromContext(filePath));
    }

}
