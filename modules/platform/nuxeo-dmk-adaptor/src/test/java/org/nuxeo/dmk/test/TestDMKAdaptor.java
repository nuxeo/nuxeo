/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.dmk.test;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.dmk-adaptor")
@Deploy("org.nuxeo.runtime.management")
public class TestDMKAdaptor {

    @Test
    public void shouldDeploy() throws InstanceNotFoundException, MalformedObjectNameException {
        MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
        ObjectInstance htmlAdaptor = mbs.getObjectInstance(new ObjectName("org.nuxeo:type=jmx-adaptor,format=html"));
        Assert.assertThat(htmlAdaptor, org.hamcrest.Matchers.notNullValue());
        ObjectInstance httpConnector = mbs.getObjectInstance(new ObjectName(
                "org.nuxeo:type=jmx-connector,protocol=jdmk-http"));
        Assert.assertThat(httpConnector, org.hamcrest.Matchers.notNullValue());
        ObjectInstance httpsConnector = mbs.getObjectInstance(new ObjectName(
                "org.nuxeo:type=jmx-connector,protocol=jdmk-https"));
        Assert.assertThat(httpsConnector, org.hamcrest.Matchers.notNullValue());
    }

}
