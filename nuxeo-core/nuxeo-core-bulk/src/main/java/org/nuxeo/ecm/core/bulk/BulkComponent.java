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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The bulk component.
 *
 * @since 10.2
 */
public class BulkComponent extends DefaultComponent {

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
    public void start(ComponentContext context) {
        super.start(context);
        bulkAdminService = new BulkAdminServiceImpl(getDescriptors(XP_ACTIONS));
        bulkService = new BulkServiceImpl();
        new ComponentListener().install();
    }

    protected class ComponentListener implements ComponentManager.Listener {
        @Override
        public void afterStart(ComponentManager mgr, boolean isResume) {
            // this is called once all components are started and ready
            ((BulkAdminServiceImpl) bulkAdminService).afterStart();
        }

        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
            // this is called before components are stopped
            if (bulkAdminService != null) {
                ((BulkAdminServiceImpl) bulkAdminService).beforeStop();
                bulkAdminService = null;
            }
            bulkService = null;
            Framework.getRuntime().getComponentManager().removeListener(this);
        }
    }
}
