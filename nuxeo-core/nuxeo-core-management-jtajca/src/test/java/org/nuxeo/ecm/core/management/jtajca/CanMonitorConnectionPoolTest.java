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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author matic
 */
@RunWith(FeaturesRunner.class)
@Features(JtajcaManagementFeature.class)
public class CanMonitorConnectionPoolTest {

    @Inject
    @Named("repository/test")
    protected ConnectionPoolMonitor repo;

    @Inject
    @Named("jdbc/repository_test")
    protected ConnectionPoolMonitor db;

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void areMonitorsInstalled() {
        isMonitorInstalled(repo);
        isMonitorInstalled(db);
    }

    @Test
    public void areConnectionsOpened() {
        isConnectionOpened(repo);
        isConnectionOpened(db);
    }

    protected void isMonitorInstalled(ConnectionPoolMonitor monitor) {
        assertThat(monitor, notNullValue());
        monitor.getConnectionCount(); // throw exception is monitor not present
    }

    protected void isConnectionOpened(ConnectionPoolMonitor monitor) {
        int count = monitor.getConnectionCount();
        if (coreFeature.getStorageConfiguration().isVCS()) {
            assertThat(count, greaterThan(0));
        } else {
            // pool is allocated but not actually used for anything
            assertEquals(0, count);
        }
    }

}
