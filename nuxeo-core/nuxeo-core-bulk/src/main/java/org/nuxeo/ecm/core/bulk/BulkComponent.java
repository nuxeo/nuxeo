/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import java.util.LinkedList;
import java.util.Queue;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The bulk component.
 *
 * @since 10.2
 */
public class BulkComponent extends DefaultComponent {

    public static final String CONFIGURATION_XP = "configuration";

    protected Queue<BulkServiceDescriptor> configurationRegistry = new LinkedList<>();

    protected BulkService bulkService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(bulkService.getClass())) {
            return (T) bulkService;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_XP.equals(extensionPoint)) {
            configurationRegistry.add((BulkServiceDescriptor) contribution);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(ComponentContext context) {
        // TODO initialize service
    }

    @Override
    public void stop(ComponentContext context) {
        bulkService = null;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_XP.equals(extensionPoint)) {
            configurationRegistry.remove(contribution);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }
}
