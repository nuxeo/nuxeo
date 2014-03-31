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
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.runtime.jtajca.management;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.lang.management.ManagementFactory;

import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import com.google.inject.Inject;

/**
 * @author matic
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.management.jtajca")
public class CanMonitorCoreSessions {

    protected CoreSessionMonitor monitor;

    @Before
    public void lookupMonitor() throws JMException {
        MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
        monitor = JMX.newMXBeanProxy(srv, new ObjectName(
                CoreSessionMonitor.NAME), CoreSessionMonitor.class);
    }


    @Inject
    CoreSession repository;

    @Inject
    FeaturesRunner runner;

    @Test
    public void isMonitorInstalled() {
        assertThat(monitor, notNullValue());
        monitor.getCount(); // throw exception is monitor not present
    }

    @Test
    public void isSessionOpened() throws ClientException {
        int count = monitor.getCount();
        assertThat(count, is(1));
        String firstInfo = monitor.getInfos()[0];
        assertThat(firstInfo.length(), greaterThan(0));
        count = monitor.getCount();
        try (CoreSession adminSession = runner.getFeature(CoreFeature.class).getRepository().openSessionAsSystemUser()) {
            assertThat(count, is(2));
            String secondInfo = monitor.getInfos()[1];
            assertThat(secondInfo, not(is(firstInfo)));
        }
        return;
    }

}
