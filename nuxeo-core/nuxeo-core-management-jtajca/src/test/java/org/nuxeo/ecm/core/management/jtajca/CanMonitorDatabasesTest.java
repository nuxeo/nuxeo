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
package org.nuxeo.ecm.core.management.jtajca;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author matic
 *
 */
@RunWith(FeaturesRunner.class)
@Features( JtajcaManagementFeature.class )
@LocalDeploy("org.nuxeo.ecm.core.management.jtajca:ds-contrib.xml")
@Ignore // databases connection are monitored through a geronimo pool (see StorageConnectionMonitor)
public class CanMonitorDatabasesTest {

    @Inject @Named("jdbc/repository_test")
    protected DatabaseConnectionMonitor monitor;

    @Inject
    CoreSession repository;

    @Test
    public void isMonitorInstalled() {
        assertThat(monitor, notNullValue());
        monitor.getMaxActive(); // throw exception is monitor not present
    }

    @Test
    public void isConnectionOpened() throws ClientException {
        int count = monitor.getNumActive();
        assertThat(count, greaterThan(0));
    }

}
