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

import java.util.List;

import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The bulk component.
 *
 * @since 10.2
 */
public class BulkComponent extends DefaultComponent implements ComponentManager.Listener {

    public static final String XP_ACTIONS = "actions";

    protected BulkService bulkService;

    protected BulkAdminService bulkAdminService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(BulkService.class)) {
            return (T) bulkService;
        } else if (adapter.isAssignableFrom(BulkAdminService.class)) {
            return (T) bulkAdminService;
        }
        return null;
    }

    @Override
    public int getApplicationStartedOrder() {
        // The Bulk Service uses a processor. The processor's topology is built using the BulkAdminService that defines
        // the Bulk Actions. Processor being contributed to the StreamService, the BulkAdminService must be started
        // before the StreamService. The StreamService is started after the KafkaConfigService, so we use the same
        // level.
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER;
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        bulkAdminService = new BulkAdminServiceImpl(getEnabledDescriptors());
        bulkService = new BulkServiceImpl();
    }

    protected List<BulkActionDescriptor> getEnabledDescriptors() {
        return getRegistryContributions(XP_ACTIONS);
    }

    @Override
    public void afterRuntimeStart(ComponentManager mgr, boolean isResume) {
        ((BulkAdminServiceImpl) bulkAdminService).afterStart();
    }

    @Override
    public void beforeRuntimeStop(ComponentManager mgr, boolean isStandby) {
        // this is called before components are stopped
        if (bulkAdminService != null) {
            ((BulkAdminServiceImpl) bulkAdminService).beforeStop();
            bulkAdminService = null;
        }
        bulkService = null;
    }

}
