/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.targetplatforms.core.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.service.DirectoryUpdater;

/**
 * @since 5.7.1
 */

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, RuntimeFeature.class })
@Deploy({ "org.nuxeo.runtime.jtajca", "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.core",
        "org.nuxeo.ecm.core.schema", "org.nuxeo.targetplatforms.core", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql" })
@LocalDeploy({ "org.nuxeo.targetplatforms.core:OSGI-INF/test-datasource-contrib.xml",
        "org.nuxeo.targetplatforms.core:OSGI-INF/test-targetplatforms-contrib.xml" })
public class TestTargetPlatformComponent {

    private static final String BUNDLE = "org.nuxeo.targetplatforms.core";

    @Inject
    protected TargetPlatformService service;

    @Inject
    protected RuntimeHarness harness;

    @Test
    public void testPlatformRegistration() throws ClientException {
        TargetPlatform tp = service.getTargetPlatform("cap-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testPackageRegistration() throws ClientException {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testOverrideDirectoryRegistration() throws Exception {
        assertEquals(DirectoryUpdater.DEFAULT_DIR, service.getOverrideDirectory());
        String contrib = "OSGI-INF/test-targetplatforms-dir-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib("org.nuxeo.targetplatforms.core", url);
            assertEquals("test", service.getOverrideDirectory());
        } finally {
            if (ctx != null) {
                ctx.undeploy(url);
            }
        }

    }

    @Test
    public void testPlatformRegistrationOverride() throws Exception {
        TargetPlatform tpOld = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tpOld);
        assertFalse(tpOld.isEnabled());

        TargetPlatform tpNew = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tpNew);
        assertTrue(tpNew.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib("org.nuxeo.targetplatforms.core", url);

            tpOld = service.getTargetPlatform("dm-5.3.0");
            assertNotNull(tpOld);
            assertTrue(tpOld.isEnabled());

            tpNew = service.getTargetPlatform("cap-5.9.2");
            assertNotNull(tpNew);
            assertFalse(tpNew.isEnabled());
        } finally {
            if (ctx != null) {
                ctx.undeploy(url);
            }
        }

    }

    @Test
    public void testPackageRegistrationOverride() throws Exception {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        RuntimeContext ctx = null;
        try {
            ctx = harness.deployTestContrib(BUNDLE, url);
            tp = service.getTargetPackage("nuxeo-dm-5.8");
            assertNotNull(tp);
            assertFalse(tp.isEnabled());

        } finally {
            if (ctx != null) {
                ctx.undeploy(url);
            }
        }
    }

}