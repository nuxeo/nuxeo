/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import junit.framework.Assert;

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
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy( { "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server" })
@LocalDeploy("org.nuxeo.ecm.automation.server:test-bindings.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class ContributionTest {

    @Inject
    AutomationServer server;

    // ------ Tests comes here --------

    /**
     * Test registration of a studio generated contribution
     */
    @Test
    public void testContribution() throws Exception {
        Assert.assertEquals(2, server.getBindings().length);
        RestBinding binding = server.getChainBinding("principals");
        Assert.assertTrue(binding.isDisabled());
        Assert.assertFalse(binding.isSecure());
        Assert.assertFalse(binding.isAdministrator());
        Assert.assertNull(binding.getGroups());
        binding = server.getChainBinding("audit");
        Assert.assertFalse(binding.isDisabled());
        Assert.assertTrue(binding.isSecure());
        Assert.assertTrue(binding.isAdministrator());
        Assert.assertEquals(1, binding.getGroups().length);
        Assert.assertEquals("members", binding.getGroups()[0]);
    }

}
