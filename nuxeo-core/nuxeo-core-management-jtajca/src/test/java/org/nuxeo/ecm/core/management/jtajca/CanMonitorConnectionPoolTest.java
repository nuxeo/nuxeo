/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
    @Named("jdbc/nuxeojunittests")
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
