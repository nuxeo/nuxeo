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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ext.MessageBodyReader;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.restapi.server.jaxrs.RoutingRequestReader;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

@RunWith(FeaturesRunner.class)
@Jetty(port = 18090)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.server.routing")
public class BodyFactoryTest extends BaseTest {

    @Inject
    WebEngine webengine;

    @Test
    public void routingExtesionsInstalled() {
        Set<? extends MessageBodyReader<?>> messageBodyReaders = APIActivator.instance.bodyFactory.getMessageBodyReaders();
        for (MessageBodyReader<?> m : messageBodyReaders) {
            if (m instanceof RoutingRequestReader) {
                return;
            }
        }
        Assert.fail("RoutingRequestReader not found");
    }

}
