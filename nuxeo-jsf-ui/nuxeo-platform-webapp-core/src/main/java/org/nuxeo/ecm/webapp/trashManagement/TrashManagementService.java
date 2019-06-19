/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.trashManagement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class TrashManagementService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.webapp.trashManagement.TrashManagementService";

    private static final Log log = LogFactory.getLog(TrashManagementService.class);

    private boolean trashManagementEnabled;

    @Override
    public void activate(ComponentContext context) {
        log.debug("TrashManagementService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.debug("TrashManagementService deactivated");
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("config".equals(extensionPoint)) {
            TrashConfigDescriptor descriptor = (TrashConfigDescriptor) contribution;
            trashManagementEnabled = descriptor.enabled;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        trashManagementEnabled = false;
    }

    public boolean isTrashManagementEnabled() {
        return trashManagementEnabled;
    }

}
