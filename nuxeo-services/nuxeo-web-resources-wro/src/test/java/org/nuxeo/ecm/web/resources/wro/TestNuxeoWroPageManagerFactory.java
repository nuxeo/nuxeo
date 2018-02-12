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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroPageCacheKeyFactory;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroPageManagerFactory;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroPageModelFactory;
import org.nuxeo.ecm.web.resources.wro.processor.CssUrlRewritingProcessor;
import org.nuxeo.ecm.web.resources.wro.provider.NuxeoUriLocator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.manager.WroManager;
import ro.isdc.wro.manager.factory.WroManagerFactory;
import ro.isdc.wro.model.factory.DefaultWroModelFactoryDecorator;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.processor.InjectorBuilder;
import ro.isdc.wro.model.resource.locator.factory.UriLocatorFactory;
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.JawrCssMinifierProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.web.resources.core")
@Deploy("org.nuxeo.web.resources.wro")
@Deploy("org.nuxeo.web.resources.wro:webresources-test-config.xml")
public class TestNuxeoWroPageManagerFactory {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterConfig mockFilterConf;

    protected WroManager victim;

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
        initMocks(this);
        Context.set(Context.webContext(mockRequest, mockResponse, mockFilterConf));
        final WroManagerFactory factory = new NuxeoWroPageManagerFactory();
        victim = factory.create();
        InjectorBuilder.create(factory).build().inject(victim);
    }

    @After
    public void tearDown() {
        Context.unset();
    }

    @Test
    public void testNuxeoWroManager() throws Exception {
        assertTrue(victim.getCacheKeyFactory() instanceof NuxeoWroPageCacheKeyFactory);
        WroModelFactory modelF = victim.getModelFactory();
        assertTrue(modelF instanceof DefaultWroModelFactoryDecorator);
        assertTrue(((DefaultWroModelFactoryDecorator) modelF).getDecoratedObject() instanceof NuxeoWroPageModelFactory);

        // ensure Nuxeo resources are detected
        UriLocatorFactory locator = victim.getUriLocatorFactory();
        String existing = Resource.PREFIX + "foldable-box.css";
        assertTrue(locator.getInstance(existing) instanceof NuxeoUriLocator);
        InputStream in = locator.locate(existing);
        assertNotNull(in);
        String unknown = Resource.PREFIX + "foo";
        assertTrue(locator.getInstance(unknown) instanceof NuxeoUriLocator);
        assertNull(locator.locate(unknown));

        // check that default processors and locators are setup
        Collection<ResourcePreProcessor> pre = new ArrayList<>(victim.getProcessorsFactory().getPreProcessors());
        assertTrue(checkProcessor(JawrCssMinifierProcessor.class, pre));
        assertTrue(checkProcessor(CssUrlRewritingProcessor.class, pre));
        List<ResourcePostProcessor> post = new ArrayList<>(victim.getProcessorsFactory().getPostProcessors());
        assertTrue(checkProcessor(JSMinProcessor.class, post));
    }

    protected boolean checkProcessor(Class<?> klass, Collection<?> procs) {
        if (procs != null) {
            for (Object proc : procs) {
                if (proc != null && proc.getClass().equals(klass)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void testNuxeoUriWildCardlocator() throws Exception {
        UriLocatorFactory locator = victim.getUriLocatorFactory();
        String existing = Resource.PREFIX + "all.css";
        assertTrue(locator.getInstance(existing) instanceof NuxeoUriLocator);
        InputStream in = locator.locate(existing);
        assertNotNull(in);
    }

}
