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
package org.nuxeo.ecm.web.resources.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy({ "org.nuxeo.web.resources.core" })
@LocalDeploy({ TestWebResourceService.BUNDLE + ":webresources-test-config.xml" })
public class TestWebResourceService {

    static final String BUNDLE = "org.nuxeo.web.resources.core";

    @Inject
    protected WebResourceManager service;

    @Inject
    protected RuntimeHarness harness;

    @Test
    public void testService() {
        assertNotNull(service);
    }

    @Test
    public void testResource() throws Exception {
        Resource r = service.getResource("jquery.js");
        assertNotNull(r);
        assertEquals("jquery.js", r.getName());
        assertEquals(ResourceType.js.name(), r.getType());
        assertEquals(0, r.getDependencies().size());
        assertEquals("scripts/jquery.js", r.getPath());

        r = service.getResource("effects");
        assertNotNull(r);
        assertEquals("effects", r.getName());
        assertEquals(ResourceType.js.name(), r.getType());
        assertEquals(0, r.getDependencies().size());
        assertEquals("scripts/effects.js", r.getPath());

        r = service.getResource("foldable-box.js");
        assertNotNull(r);
        assertEquals("foldable-box.js", r.getName());
        assertEquals(ResourceType.js.name(), r.getType());
        assertEquals(1, r.getDependencies().size());
        assertEquals("effects", r.getDependencies().get(0));
        assertEquals("scripts/foldable-box.js", r.getPath());

        r = service.getResource("foldable-box.css");
        assertNotNull(r);
        assertEquals("foldable-box.css", r.getName());
        assertEquals(ResourceType.css.name(), r.getType());
        assertEquals(0, r.getDependencies().size());
        assertEquals("css/foldable-box.css", r.getPath());

        r = service.getResource("my.css");
        assertNull(r);

        String contrib = "webresources-test-override-config.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib(BUNDLE, url);

            r = service.getResource("my.css");
            assertNotNull(r);
            assertEquals("my.css", r.getName());
            assertEquals(ResourceType.css.name(), r.getType());
            assertEquals(0, r.getDependencies().size());
            assertEquals("css/my.css", r.getPath());

        } finally {
            if (ctx != null) {
                ctx.destroy();
            }
        }

    }

    @Test
    public void testResourceBundle() throws Exception {

        ResourceBundle r = service.getResourceBundle("myapp");
        assertNotNull(r);
        assertEquals("myapp", r.getName());
        assertEquals(3, r.getResources().size());
        assertEquals("jquery.js", r.getResources().get(0));
        assertEquals("foldable-box.js", r.getResources().get(1));
        assertEquals("foldable-box.css", r.getResources().get(2));

        String contrib = "webresources-test-override-config.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib(BUNDLE, url);

            r = service.getResourceBundle("myapp");
            assertNotNull(r);
            assertEquals("myapp", r.getName());
            assertEquals(4, r.getResources().size());
            assertEquals("jquery.js", r.getResources().get(0));
            assertEquals("foldable-box.js", r.getResources().get(1));
            assertEquals("foldable-box.css", r.getResources().get(2));
            assertEquals("my.css", r.getResources().get(3));

        } finally {
            if (ctx != null) {
                ctx.destroy();
            }
        }

    }

    @Test
    public void testResources() throws Exception {
        ResourceContext ctx = new ResourceContextImpl();
        List<Resource> res = service.getResources(ctx, "foo", null);
        assertNotNull(res);
        assertEquals(0, res.size());

        // test cycle detection
        res = service.getResources(ctx, "cycles", null);
        assertNotNull(res);
        assertEquals(3, res.size());
        assertEquals("cycle3.js", res.get(0).getName());
        assertEquals("cycle2.js", res.get(1).getName());
        assertEquals("cycle1.js", res.get(2).getName());

        res = service.getResources(ctx, "myapp", ResourceType.any.name());
        assertNotNull(res);
        assertEquals(4, res.size());
        assertEquals("jquery.js", res.get(0).getName());
        assertEquals("effects", res.get(1).getName());
        assertEquals("foldable-box.js", res.get(2).getName());
        assertEquals("foldable-box.css", res.get(3).getName());

        res = service.getResources(ctx, "myapp", ResourceType.js.name());
        assertNotNull(res);
        assertEquals(3, res.size());
        assertEquals("jquery.js", res.get(0).getName());
        assertEquals("effects", res.get(1).getName());
        assertEquals("foldable-box.js", res.get(2).getName());

        res = service.getResources(ctx, "myapp", ResourceType.css.name());
        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("foldable-box.css", res.get(0).getName());

    }

    @Test
    public void testProcessors() throws Exception {
        Processor p = service.getProcessor("myProc");
        assertNotNull(p);
        assertEquals(10, p.getOrder());
        assertEquals(MockProcessor.class, p.getTargetProcessorClass());
        assertEquals(1, p.getTypes().size());
        assertEquals("wroPost", p.getTypes().get(0));

        List<Processor> procs = service.getProcessors("wroPost");
        assertEquals(1, procs.size());
        assertEquals(p, procs.get(0));

        String contrib = "webresources-test-override-config.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib(BUNDLE, url);

            p = service.getProcessor("myProc");
            assertNull(p);
            procs = service.getProcessors("wroPost");
            assertEquals(0, procs.size());

        } finally {
            if (ctx != null) {
                ctx.destroy();
            }
        }

    }

}
