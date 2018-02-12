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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroPageModelFactory;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.DefaultWroModelFactoryDecorator;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.util.WroTestUtils;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.web.resources.core")
@Deploy("org.nuxeo.theme.styling")
@Deploy("org.nuxeo.web.resources.wro")
@Deploy("org.nuxeo.web.resources.wro:webresources-test-config.xml")
public class TestNuxeoWroPageModelFactory {

    static final String BUNDLE = "org.nuxeo.web.resources.rest";

    protected WroModel model;

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
        Context.set(Context.standaloneContext());
        WroModelFactory factory = DefaultWroModelFactoryDecorator.decorate(new NuxeoWroPageModelFactory(),
                Collections.emptyList());
        WroTestUtils.init(factory);
        model = factory.create();
    }

    @After
    public void tearDown() {
        Context.unset();
    }

    @Test
    public void testNuxeoWroPageModelBundle() throws Exception {
        Collection<Group> groups = model.getGroups();
        assertEquals(1, groups.size());
        Iterator<Group> groupsIt = model.getGroups().iterator();

        Group page = groupsIt.next();
        assertNotNull(page);
        assertEquals("galaxy/default", page.getName());
        assertTrue(page.hasResourcesOfType(ResourceType.CSS));
        assertTrue(page.hasResourcesOfType(ResourceType.JS));
        List<Resource> allResources = page.getResources();
        assertEquals(8, allResources.size());
        assertEquals("nuxeo:jquery.js", allResources.get(0).getUri());
        assertEquals("nuxeo:effects", allResources.get(1).getUri());
        assertEquals("nuxeo:foldable-box.js", allResources.get(2).getUri());
        assertEquals("nuxeo:foldable-box.css", allResources.get(3).getUri());
        assertEquals("nuxeo:nuxeo_dm_default.css", allResources.get(4).getUri());
        assertEquals("nuxeo:all.css", allResources.get(5).getUri());
        assertEquals("nuxeo:all.js", allResources.get(6).getUri());
        assertEquals("nuxeo:cycle3.js", allResources.get(7).getUri());
    }

}
