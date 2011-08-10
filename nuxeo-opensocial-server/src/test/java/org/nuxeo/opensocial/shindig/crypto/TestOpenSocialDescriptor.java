/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.shindig.crypto;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_LOOPBACK_URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestOpenSocialDescriptor {

    @Test
    public void trustedHostShouldBeEqualsToLoopbackIP() {
        Framework.getProperties().put(PARAM_LOOPBACK_URL,
                "http://127.0.0.1:8080/nuxeo");
        OpenSocialDescriptor openSocialDescriptor = new OpenSocialDescriptor();
        String trustedHost = openSocialDescriptor.getTrustedHostFromLoopbackURL();
        assertEquals("127.0.0.1", trustedHost);

        Framework.getProperties().put(PARAM_LOOPBACK_URL,
                "http://10.213.2.105:8080/nuxeo");
        trustedHost = openSocialDescriptor.getTrustedHostFromLoopbackURL();
        assertEquals("10.213.2.105", trustedHost);

        Framework.getProperties().put(PARAM_LOOPBACK_URL,
                "http://[0:0:0:0:0:0:0:1]:8080/nuxeo");
        trustedHost = openSocialDescriptor.getTrustedHostFromLoopbackURL();
        assertEquals("0:0:0:0:0:0:0:1", trustedHost);

        Framework.getProperties().put(PARAM_LOOPBACK_URL,
                "http://[2a01:240:fe8e:0:226:bbff:fe09:55cd]:8080/nuxeo");
        trustedHost = openSocialDescriptor.getTrustedHostFromLoopbackURL();
        assertEquals("2a01:240:fe8e:0:226:bbff:fe09:55cd", trustedHost);
    }

    @Test
    public void loopbackIPShouldBeAddedTotheConfiguredListOfTrustedHosts() {
        Framework.getProperties().put(PARAM_LOOPBACK_URL,
                "http://10.213.2.105:8080/nuxeo");
        OpenSocialDescriptor openSocialDescriptor = new OpenSocialDescriptor();
        String trustedHost = openSocialDescriptor.getTrustedHostFromLoopbackURL();
        assertEquals("10.213.2.105", trustedHost);

        openSocialDescriptor.setTrustedHosts("host1,host2,10.0.40.40");
        String[] trustedHosts = openSocialDescriptor.getTrustedHosts();
        assertEquals(4, trustedHosts.length);
        assertEquals("10.213.2.105", trustedHosts[0]);
        assertEquals("host1", trustedHosts[1]);
        assertEquals("host2", trustedHosts[2]);
        assertEquals("10.0.40.40", trustedHosts[3]);
    }

}
