/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.jtajca;

import java.util.ArrayList;
import java.util.List;
import javax.resource.ResourceException;

import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.ConnectionTrackingInterceptor;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTracker;

public class NuxeoConnectionTrackingCoordinator implements ConnectionTracker {

    private final List<ConnectionTracker> trackers = new ArrayList<>();

    public void addTracker(ConnectionTracker tracker) {
        trackers.add(tracker);
    }

    public void removeTracker(ConnectionTracker tracker) {
        trackers.remove(tracker);
    }

    @Override
    public void handleObtained(ConnectionTrackingInterceptor connectionTrackingInterceptor,
            ConnectionInfo connectionInfo, boolean reassociate) throws ResourceException {
        for (ConnectionTracker tracker : trackers) {
            tracker.handleObtained(connectionTrackingInterceptor, connectionInfo, reassociate);
        }
    }

    @Override
    public void handleReleased(ConnectionTrackingInterceptor connectionTrackingInterceptor,
            ConnectionInfo connectionInfo, ConnectionReturnAction connectionReturnAction) {
        for (ConnectionTracker tracker : trackers) {
            tracker.handleReleased(connectionTrackingInterceptor, connectionInfo, connectionReturnAction);
        }
    }

    @Override
    public void setEnvironment(ConnectionInfo connectionInfo, String key) {
        for (ConnectionTracker tracker : trackers) {
            tracker.setEnvironment(connectionInfo, key);
        }
    }

}