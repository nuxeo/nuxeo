/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.RestBinding;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.io",
        "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.webengine.core" })
@LocalDeploy("org.nuxeo.ecm.automation.server:test-bindings.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class ContributionTest {

    @Inject
    AutomationServer server;

    // ------ Tests comes here --------

    public static final int DEFAULT_BINDINGS = 8;

    /**
     * Test registration of a studio generated contribution
     */
    @Test
    public void testContribution() {
        assertEquals(DEFAULT_BINDINGS + 2, server.getBindings().length);
        RestBinding binding = server.getChainBinding("principals");

        assertTrue(binding.isDisabled());
        assertFalse(binding.isSecure());
        assertFalse(binding.isAdministrator());
        assertNull(binding.getGroups());

        binding = server.getChainBinding("audit");
        assertFalse(binding.isDisabled());
        assertTrue(binding.isSecure());
        assertTrue(binding.isAdministrator());
        assertEquals(1, binding.getGroups().length);
        assertEquals("members", binding.getGroups()[0]);
    }

}
