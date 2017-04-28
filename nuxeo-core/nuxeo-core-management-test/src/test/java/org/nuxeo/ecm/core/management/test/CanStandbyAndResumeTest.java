/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.test;

import javax.management.JMX;
import javax.management.MBeanServer;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.management.standby.StandbyCommand;
import org.nuxeo.ecm.core.management.standby.StandbyMXBean;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 *
 *
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.runtime.management", //
        "org.nuxeo.ecm.core.management" })
public class CanStandbyAndResumeTest {

    @Test
    public void canCommand() throws InterruptedException {
        MBeanServer server = Framework.getService(ServerLocator.class).lookupServer();
        StandbyMXBean bean = JMX.newMBeanProxy(server, ObjectNameFactory.getObjectName(StandbyCommand.class.getName()),
                StandbyMXBean.class);
        Assertions.assertThat(bean.isStandby()).isFalse();
        bean.standby(10);
        Assertions.assertThat(bean.isStandby()).isTrue();
        bean.resume();
        Assertions.assertThat(bean.isStandby()).isFalse();
    }

}
